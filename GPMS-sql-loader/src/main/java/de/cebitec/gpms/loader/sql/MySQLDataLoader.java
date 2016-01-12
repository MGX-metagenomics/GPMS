/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.loader.sql;

import de.cebitec.gpms.data.GPMSDataLoaderI;
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
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.db.DataSourceFactory;
import de.cebitec.gpms.db.GPMSMaster;
import de.cebitec.gpms.db.GPMSProxyDataSource;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.gpms.model.DBAPIType;
import de.cebitec.gpms.model.DBMSType;
import de.cebitec.gpms.model.DataSourceType;
import de.cebitec.gpms.model.GPMSDataSourceDB;
import de.cebitec.gpms.model.GPMSHost;
import de.cebitec.gpms.model.Membership;
import de.cebitec.gpms.model.Project;
import de.cebitec.gpms.model.ProjectClass;
import de.cebitec.gpms.model.Role;
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
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class MySQLDataLoader implements GPMSDataLoaderI {

    @Resource(mappedName = "jdbc/GPMS")
    private DataSource gpmsds;
    //
    @EJB
    private GPMSConfiguration config;

    //
    private ProxyDataSourceI proxyDS = null;
    private EntityManagerFactory emf = null;
    private final ThreadLocal<JPAMasterI> currentMaster = new ThreadLocal<>();
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
    }

    @Override
    public JPAMasterI createMaster(MembershipI mbr) {

        String[] dbAuth = getDatabaseCredentials(mbr.getRole());

        // find an appropriate GPMS datasource to work with
        DataSource_DBI selectedGPMSdataSource = null;
        for (DataSourceI gpmsDS : mbr.getProject().getDataSources()) {
            if (gpmsDS instanceof DataSource_DBI) {
                DataSource_DBI dsdb = (DataSource_DBI) gpmsDS;
                if (MGX_DS_TYPE.equals(dsdb.getType()) && MGX_DBAPI_TYPE.equals(dsdb.getAPIType())) {
                    selectedGPMSdataSource = dsdb;
                    break;
                }
            }
        }

        if (selectedGPMSdataSource == null) {
            throw new RuntimeException("No appropriate SQL datasource could be found for project " + mbr.getProject().getName());
        }

        DataSource ds = DataSourceFactory.createDataSource(mbr, selectedGPMSdataSource, dbAuth[0], dbAuth[1]);
        GPMSMaster master = new GPMSMaster(mbr, selectedGPMSdataSource, ds);

        // current master needs to be set _before_ creating the EMF
        currentMaster.set(master);

        if (emf == null) {
            emf = EMFNameResolver.createEMF(mbr, ProxyDataSourceI.JNDI_NAME, "MGX-PU");
        }

        master.setEntityManagerFactory(emf);
        return master;
    }

    @Override
    public JPAMasterI getCurrentMaster() {
        return currentMaster.get();
    }

    @Override
    public void setCurrentMaster(JPAMasterI master) {
        currentMaster.set(master);
    }

    private final static String SQL_GET_MEMBERSHIPS
            = new StringBuilder("SELECT Project.name AS pName, Role.name AS rName, Project_Class.name AS pcName FROM User ")
            .append("LEFT JOIN Member on (User._id = Member.user_id) ")
            .append("LEFT JOIN Project on (Member.project_id = Project._id) ")
            .append("LEFT JOIN Project_Class on (Project.project_class_id = Project_Class._id) ")
            .append("LEFT JOIN Role on (Member.role_id = Role._id) ")
            .append("WHERE User.login=?")
            .toString();

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
        List<MembershipI> ret = new ArrayList<>();

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_MEMBERSHIPS)) {
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
                        }
                        Collection<DataSourceI> projectDataSources = loadDataSources(projectName, MGX_DS_TYPE);
                        ProjectI project = new Project(projectName, projectClass, projectDataSources, false);

                        RoleI targetRole = null;
                        for (RoleI role : projectClass.getRoles()) {
                            if (role.getName().equals(roleName)) {
                                targetRole = role;
                                break;
                            }
                        }

                        if (targetRole != null) {
                            ret.add(new Membership(project, targetRole));
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
        membership_cache.put(userLogin, ret);
        return ret;
    }

    private String[] getDatabaseCredentials(RoleI role) {
        return dbAccess.get(role);
    }

    private final static DBAPITypeI MGX_DBAPI_TYPE = new DBAPIType("MGX");
    private final static DBAPITypeI REST_DBAPI_TYPE = new DBAPIType("REST");
    private final static DataSourceTypeI MGX_DS_TYPE = new DataSourceType("MGX");

    private final static String sql = "SELECT Host.hostname AS host, Host.port AS port, "
            + "DBMS_Type.name AS dbmsname, DBMS_Type.version_ AS dbmsver, "
            + "DataSource.name AS datasource_name "
            + "FROM Project "
            + "LEFT JOIN Project_datasources ON (Project._id = Project_datasources._parent_id) "
            + "LEFT JOIN DataSource ON (Project_datasources._array_value = DataSource._id) "
            + "LEFT JOIN DataSource_Type ON (DataSource.datasource_type_id = DataSource_Type._id) "
            + "LEFT JOIN DataSource_DB ON (DataSource._id = DataSource_DB._parent_id) "
            + "LEFT JOIN Host ON (DataSource_DB.host_id = Host._id) "
            + "LEFT JOIN DBMS_Type ON (DataSource_DB.dbms_type_id = DBMS_Type._id) "
            + "WHERE DataSource_Type.name=? AND Project.name=?";

    private Collection<DataSourceI> loadDataSources(String projectName, DataSourceTypeI dsType) throws SQLException {

        List<DataSourceI> datasources = new ArrayList<>();

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dsType.getName());
                stmt.setString(2, projectName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        HostI host = new GPMSHost(rs.getString(1), rs.getInt(2));
                        DBMSTypeI dbms = new DBMSType(rs.getString(3), rs.getInt(4));
                        DataSource_DBI dataSource = new GPMSDataSourceDB(rs.getString(5), MGX_DS_TYPE, dbms, MGX_DBAPI_TYPE, host);
                        datasources.add(dataSource);
                    }
                }
            }
        }
        return datasources;
    }

    private Collection<RoleI> loadRoles(ProjectClassI pClass) throws GPMSException {

        String cfgFileName = new StringBuilder(config.getGPMSConfigDirectory()).append(File.separator).append(pClass.getName().toLowerCase()).append(".conf").toString();
        File cfgFile = new File(cfgFileName);
        if (!cfgFile.exists()) {
            throw new GPMSException(cfgFile.getAbsolutePath() + " missing or unreadable.");
        }

        log("Reading " + pClass.getName() + " role file " + cfgFile.getAbsolutePath());

        List<RoleI> ret = new ArrayList<>(3);

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
                    String[] strings = line.split(":");
                    if (strings.length != 3) {
                        log("Unparseable line in " + cfgFileName + ": " + line);
                        throw new GPMSException("Invalid format for application configuration file.");
                    }

                    Role r = new Role(pClass, strings[0]);
                    pClass.getRoles().add(r);

                    String dbUser = strings[1];
                    String dbPass = strings[2];
                    ret.add(r);
                    dbAccess.put(r, new String[]{dbUser, dbPass});
                }
            }
        } catch (IOException ex) {
            log(ex.getMessage());
            ret.clear();
            throw new GPMSException(ex);
        }

        return ret;
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
        return supportedProjectClasses.values();
    }

}
