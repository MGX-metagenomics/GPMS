/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.loader.sql;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.cebitec.gpms.core.DBAPITypeI;
import de.cebitec.gpms.core.DBMSTypeI;
import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSourceTypeI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.HostI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.db.GPMSProxyDataSource;
import de.cebitec.gpms.db.GPMSDataLoader;
import de.cebitec.gpms.model.DBAPIType;
import de.cebitec.gpms.model.DBMSType;
import de.cebitec.gpms.model.DataSourceType;
import de.cebitec.gpms.model.GPMSDataSourceDB;
import de.cebitec.gpms.model.GPMSHost;
import de.cebitec.gpms.model.Membership;
import de.cebitec.gpms.model.Project;
import de.cebitec.gpms.model.ProjectClass;
import de.cebitec.gpms.model.Role;
import de.cebitec.gpms.util.GPMSDataLoaderI;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class MySQLDataLoader extends GPMSDataLoader implements GPMSDataLoaderI {

    @Resource(mappedName = "jdbc/GPMS")
    private DataSource gpmsds;
    //
    @EJB
    private GPMSConfiguration config;

    //
    private ProxyDataSourceI proxyDS = null;
    //
    // caches
    //
    private static Cache<String, List<MembershipI>> membership_cache;
    private static final ConcurrentMap<RoleI, String[]> dbAccess = new ConcurrentHashMap<>(5);
    //
    private static final ConcurrentMap<String, ProjectClassI> supportedProjectClasses = new ConcurrentHashMap<>(5);

    @PostConstruct
    public void start() {
        membership_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();

        // preload project class and roles
        try {
            ProjectClassI mgxPClass = new ProjectClass("MGX");
            loadRoles(mgxPClass);
            supportedProjectClasses.put("MGX", mgxPClass);
        } catch (GPMSException ex) {
            Logger.getLogger(MySQLDataLoader.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        // publish datasource for JPA in JNDI
        proxyDS = new GPMSProxyDataSource(this);
        try {
            Context ctx = new InitialContext();
            ctx.rebind(ProxyDataSourceI.JNDI_NAME, proxyDS);
        } catch (NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void stop() {
        if (proxyDS != null) {
            try {
                Context ctx = new InitialContext();
                ctx.unbind(ProxyDataSourceI.JNDI_NAME);
                proxyDS = null;
            } catch (NamingException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.dispose();
    }

    private final static String SQL_GET_MEMBERSHIPS_BY_LOGIN
            = "SELECT Project.name AS pName, Role.name AS rName, Project_Class.name AS pcName FROM User "
            + "LEFT JOIN Member on (User._id = Member.user_id) "
            + "LEFT JOIN Project on (Member.project_id = Project._id) "
            + "LEFT JOIN Project_Class on (Project.project_class_id = Project_Class._id) "
            + "LEFT JOIN Role on (Member.role_id = Role._id) "
            + "WHERE User.login=?";

    @Override
    public Collection<MembershipI> getMemberships(final String userLogin) throws GPMSException {
        // cache lookup first
        //
        List<MembershipI> cachedMemberships = membership_cache.getIfPresent(userLogin);
        if (cachedMemberships != null) {
            return cachedMemberships;
        }

        // no cache entry, have to do the lookup
        //
        List<MembershipI> memberships = new ArrayList<>();

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_MEMBERSHIPS_BY_LOGIN)) {
                stmt.setString(1, userLogin);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {

                        String projectName = rs.getString(1);
                        String roleName = rs.getString(2);
                        String projectClassname = rs.getString(3);

                        ProjectClassI projectClass = supportedProjectClasses.get(projectClassname);
                        if (projectClass == null) {
                            log("Found unsupported project class " + projectClassname + ", loading..");
                            projectClass = new ProjectClass(projectClassname);
                            loadRoles(projectClass); // load role definitions
                            supportedProjectClasses.put(projectClassname, projectClass);
                        }
                        Collection<DataSourceI> projectDataSources = loadDataSources(projectName);
                        ProjectI project = new Project(projectName, projectClass, projectDataSources, false);

                        RoleI targetRole = null;
                        for (RoleI role : projectClass.getRoles()) {
                            if (role.getName().equals(roleName)) {
                                targetRole = role;
                                break;
                            }
                        }

                        if (targetRole != null) {
                            memberships.add(new Membership(project, targetRole));
                        } else {
                            log("Invalid role name {0} for {1} in project {2}", roleName, userLogin, project.getName());
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getMessage());
            return Collections.EMPTY_LIST; // empty list instead of incomplete one
        }

        // cache membership list
        membership_cache.put(userLogin, memberships);
        return memberships;
    }

    private final static String SQL_GET_PROJCLASS_BY_PROJECT_NAME = "SELECT Project_Class.name FROM Project "
            + "LEFT JOIN Project_Class ON (Project.project_class_id = Project_Class._id) "
            + "WHERE Project.name=?";

    @Override
    public ProjectI getProject(String projectName) throws GPMSException {
        ProjectI project = null;
        try {

            String pClassName = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_PROJCLASS_BY_PROJECT_NAME)) {
                    stmt.setString(1, projectName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            pClassName = rs.getString(1);
                        }
                    }
                }
            }
            if (pClassName == null) {
                throw new GPMSException("No such project");
            }

            Collection<DataSourceI> dataSources = loadDataSources(projectName);

            ProjectClassI pClass = new ProjectClass(projectName);
            project = new Project(projectName, pClass, dataSources, false);
        } catch (SQLException ex) {
            Logger.getLogger(MySQLDataLoader.class.getName()).log(Level.SEVERE, null, ex);
            throw new GPMSException(ex);
        }
        return project;
    }

    @Override
    public String[] getDatabaseCredentials(RoleI role) throws GPMSException {
        String[] ret = dbAccess.get(role);
        if (ret == null || ret.length != 2) {
            throw new GPMSException("No credentials available.");
        }
        return ret;
    }

    private final static String SQL_LOAD_DATASOURCES_BY_PROJECT = "SELECT Host.hostname AS host, Host.port AS port, "
            + "DBMS_Type.name AS dbmsname, DBMS_Type.version_ AS dbmsver, "
            + "DataSource.name AS dsName, DataSource_Type.name AS dsTypeName, "
            + "DB_API_Type.name AS dbApiName "
            + "FROM Project "
            + "JOIN Project_datasources ON (Project._id = Project_datasources._parent_id) "
            + "LEFT JOIN DataSource ON (Project_datasources._array_value = DataSource._id) "
            + "LEFT JOIN DataSource_Type ON (DataSource.datasource_type_id = DataSource_Type._id) "
            + "LEFT JOIN DataSource_DB ON (DataSource._id = DataSource_DB._parent_id) "
            + "LEFT JOIN Host ON (DataSource_DB.host_id = Host._id) "
            + "LEFT JOIN DBMS_Type ON (DataSource_DB.dbms_type_id = DBMS_Type._id) "
            + "LEFT JOIN DB_API_Type ON (DataSource_DB.db_api_type_id = DB_API_Type._id) "
            + "WHERE Project.name=?";

    private Collection<DataSourceI> loadDataSources(String projectName) throws SQLException {

        List<DataSourceI> datasources = new ArrayList<>(1);

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_LOAD_DATASOURCES_BY_PROJECT)) {
                stmt.setString(1, projectName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        HostI host = new GPMSHost(rs.getString(1), rs.getInt(2));
                        DBMSTypeI dbms = new DBMSType(rs.getString(3), rs.getString(4));
                        String datasourceName = rs.getString(5);
                        DataSourceTypeI dsType = new DataSourceType(rs.getString(6));
                        DBAPITypeI apiType = new DBAPIType(rs.getString(7));
                        DataSource_DBI dataSource = new GPMSDataSourceDB(datasourceName, dsType, dbms, apiType, host);
                        datasources.add(dataSource);
                    }
                }
            }
        }
        return datasources;
    }

    private void loadRoles(ProjectClassI pClass) throws GPMSException {

        String cfgFileName = new StringBuilder(config.getGPMSConfigDirectory()).append(File.separator).append(pClass.getName().toLowerCase()).append(".conf").toString();
        File cfgFile = new File(cfgFileName);
        if (!cfgFile.exists()) {
            log(cfgFile.getAbsolutePath() + " missing or unreadable, obtaining roles for " + pClass.getName() + " from SQL database.");
            loadRolesFromDB(pClass);
            return;
        }

        log("Reading " + pClass.getName() + " role file " + cfgFile.getAbsolutePath());

        String line;
        boolean in_section = false;
        try (BufferedReader br = new BufferedReader(new FileReader(cfgFileName))) {
            while ((line = br.readLine()) != null) {
                if (line.contains("<Role_Accounts>")) {
                    in_section = true;
                    continue;
                } else if (line.contains("</Role_Accounts>")) {
                    in_section = false;
                    continue;
                }

                if (in_section) {
                    line = line.trim();
                    if (line.contains(":") && !line.startsWith("#")) {
                        String[] strings = line.split(":");
                        if (strings.length != 3) {
                            log("Unparseable line in " + cfgFileName + ": " + line);
                            throw new GPMSException("Invalid format for application configuration file.");
                        }

                        Role r = new Role(pClass, strings[0]);
                        pClass.getRoles().add(r);

                        String dbUser = strings[1];
                        String dbPass = strings[2];
                        dbAccess.put(r, new String[]{dbUser, dbPass});
                    }
                }
            }
        } catch (IOException ex) {
            log(ex.getMessage());
            throw new GPMSException(ex);
        }

    }

    private final static String SQL_ROLES_BY_PROJCLASSNAME = "SELECT r.name FROM Role r "
            + "LEFT JOIN Project_Class pc ON (r.project_class_id=pc._id) "
            + "WHERE pc.name=?";

    private void loadRolesFromDB(ProjectClassI pClass) throws GPMSException {

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_ROLES_BY_PROJCLASSNAME)) {
                stmt.setString(1, pClass.getName());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String roleName = rs.getString(1);
                        log(pClass.getName() + ": found DB role: " + roleName);
                        RoleI r = new Role(pClass, roleName);
                        pClass.getRoles().add(r);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getMessage());
            throw new GPMSException(ex);
        }
    }

    private Connection getConnection() throws SQLException {
        return gpmsds.getConnection();
    }

    private final static Logger logger = Logger.getLogger(MySQLDataLoader.class.getPackage().getName());

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    private void log(String msg, Object... args) {
        logger.log(Level.INFO, String.format(msg, args));
    }

    @Override
    public Collection<ProjectClassI> getSupportedProjectClasses() {
        Collection<ProjectClassI> ret = new ArrayList<>(supportedProjectClasses.size());
        //
        //  filter all seen project classes and return only those where
        // all roles have valid database access credentials
        //
        for (ProjectClassI pClass : supportedProjectClasses.values()) {
            boolean allRolesAccessible = true;
            for (RoleI role : pClass.getRoles()) {
                String[] databaseCredentials = null;
                try {
                    databaseCredentials = getDatabaseCredentials(role);
                } catch (GPMSException ex) {
                    allRolesAccessible = false;
                }
                if (databaseCredentials == null) {
                    allRolesAccessible = false;
                    break;
                }
            }

            if (allRolesAccessible) {
                ret.add(pClass);
            }
        }
        return ret;
    }

}
