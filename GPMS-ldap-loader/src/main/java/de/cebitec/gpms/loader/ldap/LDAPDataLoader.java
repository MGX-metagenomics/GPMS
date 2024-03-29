package de.cebitec.gpms.loader.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import de.cebitec.gpms.core.DBAPITypeI;
import de.cebitec.gpms.core.DBMSTypeI;
import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSourceTypeI;
import de.cebitec.gpms.core.DataSource_ApplicationServerI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.GPMSMessageI;
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
import de.cebitec.gpms.model.GPMSDataSourceAppServer;
import de.cebitec.gpms.model.GPMSDataSourceDB;
import de.cebitec.gpms.model.GPMSHost;
import de.cebitec.gpms.model.GPMSMessage;
import de.cebitec.gpms.model.Membership;
import de.cebitec.gpms.model.Project;
import de.cebitec.gpms.model.ProjectClass;
import de.cebitec.gpms.model.Role;
import de.cebitec.gpms.util.GPMSDataLoaderI;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class LDAPDataLoader extends GPMSDataLoader implements GPMSDataLoaderI {

    //
    @EJB
    private GPMSConfiguration config;
    //
    // set to true to skip DataSource_AS instances
    private final boolean IGNORE_APPSERV = true;
    //
    private final static String baseDN = "dc=computational,dc=bio,dc=uni-giessen,dc=de";
    private final static String gpmsBaseDN = "ou=gpms,ou=services," + baseDN;
    private final static String userBaseDN = "ou=users,dc=computational,dc=bio,dc=uni-giessen,dc=de";
    private final static String projClassBaseDN = "ou=Project_Class," + gpmsBaseDN;
    //
    private LDAPConnectionPool ldapPool;
    //
    private ProxyDataSourceI proxyDS = null;
    //
    // caches
    //
    private static Cache<String, List<MembershipI>> membership_cache;
    private static Cache<String, HostI> host_cache;
    private static Cache<String, DataSourceTypeI> dstype_cache;
    private static Cache<String, DBMSTypeI> dbmstype_cache;
    private static Cache<String, DBAPITypeI> apitype_cache;
    //
    //
    //
    private static final ConcurrentMap<RoleI, String[]> dbAccess = new ConcurrentHashMap<>(5);
    //
    private static final ConcurrentMap<String, ProjectClassI> supportedProjectClasses = new ConcurrentHashMap<>(5);

    //
    // LDAP attributes
    private final static String ATTR_DN = "dn";
    private final static String ATTR_NAME = "name";
    private final static String ATTR_OBJECTCLASS = "objectClass";
    private final static String ATTR_HOSTNAME = "gpmsHostName";
    private final static String ATTR_HOSTPORT = "gpmsPort";
    //
    // LDAP DN references
    //
    private final static String ATTR_GPMSROLE = "gpmsRole";
    private final static String ATTR_GPMSHOST = "gpmsHost";
    private final static String ATTR_PROJECTCLASS = "gpmsProjectClass";
    private final static String ATTR_DBMSTYPE = "gpmsDBMSType";
    private final static String ATTR_APITYPE = "gpmsAPIType";
    private final static String ATTR_URL = "gpmsURL";
    private final static String ATTR_DATASOURCETYPE = "gpmsDataSourceType";
    private final static String ATTR_DATASOURCE = "gpmsDataSource";
    private final static String ATTR_VERSION = "gpmsVersion";

    @PostConstruct
    public void start() {
        // assume repeated  invocation of start method
        if (ldapPool != null) {
            return;
        }

        // setup ldap connection pool
        RoundRobinServerSet serverSet = new RoundRobinServerSet(
                new String[]{
                    //                    "localhost"//,
                    "infra.internal.computational.bio.uni-giessen.de"
                }, new int[]{389}
        );
        try {
            BindRequest breq = new SimpleBindRequest("cn=gpms_access," + baseDN, "gpms");
            ldapPool = new LDAPConnectionPool(serverSet, breq, 10); // unauthenticated connection
            ldapPool.setMaxWaitTimeMillis(5000);
            ldapPool.setMaxConnectionAgeMillis(10000);
        } catch (LDAPException ex) {
            Logger.getLogger(LDAPDataLoader.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        //
        // initialize caches
        //
        membership_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
        dstype_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
        dbmstype_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
        apitype_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();
        host_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();

        // preload MGX data
        try {
            String pClassDN = "name=MGX,ou=Project_Class,ou=gpms,ou=services,dc=computational,dc=bio,dc=uni-giessen,dc=de";
            getProjectClass(pClassDN);
        } catch (GPMSException ex) {
            Logger.getLogger(LDAPDataLoader.class.getName()).log(Level.SEVERE, null, ex);
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

        // shutdown ldap pool
        if (ldapPool != null) {
            ldapPool.close();
            ldapPool = null;
        }
        super.dispose();
    }

    @Override
    public List<GPMSMessageI> getMessages() {
        List<GPMSMessageI> ret = new ArrayList<>();
        File newsDirectory = new File(new File(config.getGPMSConfigDirectory()), "NEWS");
        if (!newsDirectory.exists()) {
            if (!newsDirectory.mkdirs()) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Cannot create NEWS directory!");
                return ret;
            }
        }

        File[] listFiles = newsDirectory.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return ret;
        }

        for (File f : listFiles) {
            if (f.isFile() && f.canRead()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                    GPMSMessage g = new GPMSMessage(new Date(f.lastModified() / 1000L), sb.toString());
                    ret.add(g);
                } catch (IOException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return ret;
    }

    @Override
    public Collection<MembershipI> getMemberships(final String userLogin) throws GPMSException {
        if (userLogin == null || userLogin.contains("*")) {
            log("Invalid user login attempt for login " + (userLogin == null ? "null" : userLogin));
            throw new GPMSException("Invalid login!");
        }
        //
        // cache lookup first
        //
        List<MembershipI> cachedMemberships = membership_cache.getIfPresent(userLogin);
        if (cachedMemberships != null) {
            return cachedMemberships;
        }

        // no cache entry, have to do the lookup
        //
        List<MembershipI> ret = new ArrayList<>();

        LDAPConnection conn = null;

        try {
            conn = getConnection();

            //
            // find the user
            //
            Filter userFilter = Filter.createORFilter(
                    Filter.create(String.format("(&(objectClass=gpmsExternalUser)(cn=%s))", userLogin)),
                    Filter.create(String.format("(&(objectClass=gpmsInternalUser)(cn=%s))", userLogin)));
            ReadOnlySearchRequest userReq = new SearchRequest(userBaseDN, SearchScope.SUB, userFilter, ATTR_DN);
            final SearchResult userResult = conn.search(userReq);
            if (userResult.getEntryCount() == 0) {
                throw new GPMSException("No such user");
            } else if (userResult.getEntryCount() > 1) {
                log("Unexpected number of results: " + userResult.getEntryCount() + " for login " + userLogin);
                throw new GPMSException("Could not determine user");
            }
            final String userDN = userResult.getSearchEntries().get(0).getDN();

            //
            // fetch memberships
            //
            Filter membershipFilter = Filter.create(String.format("(&(objectClass=gpmsMember)(gpmsUser=%s))", userDN));
            final SearchResult membershipResult = conn.search(new SearchRequest(String.format("ou=Project,%s", gpmsBaseDN), SearchScope.SUB, membershipFilter, ATTR_GPMSROLE));

            for (SearchResultEntry sre : membershipResult.getSearchEntries()) {
                if (sre.hasAttribute(ATTR_GPMSROLE)) {
                    final String projectDN = sre.getParentDNString();
                    ProjectI project = getProjectByDN(projectDN);

                    if (project != null) {
                        RoleI targetRole = null;
                        final String roleDN = sre.getAttributeValue(ATTR_GPMSROLE);
                        String roleName = conn.getEntry(roleDN, ATTR_NAME).getAttributeValue(ATTR_NAME);
                        for (RoleI role : project.getProjectClass().getRoles()) {
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
        } catch (LDAPException ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getMessage());
            return Collections.EMPTY_LIST; // empty list instead of incomplete one
        } finally {
            if (conn != null) {
                conn.close();
            }

        }

        // cache membership list
        membership_cache.put(userLogin, ret);
        return ret;
    }

    private String getProjectDN(String projName) throws GPMSException {

        if (projName == null || projName.isEmpty()) {
            throw new GPMSException("Unable to handle null or empty project name.");
        }

        LDAPConnection conn = null;
        String projectDN = null;

        try {
            conn = getConnection();
            Filter projectFilter = Filter.create(String.format("(&(objectClass=gpmsProject)(name=%s))", projName));
            final SearchResult projectResult = conn.search(new SearchRequest(String.format("ou=Project,%s", gpmsBaseDN), SearchScope.SUB, projectFilter, "dn"));
            if (projectResult.getEntryCount() != 1) {
                throw new GPMSException("Could not find project " + projName);
            }
            projectDN = projectResult.getSearchEntries().get(0).getDN();

        } catch (LDAPException ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getMessage());
            throw new GPMSException(ex);
        } finally {
            if (conn != null) {
                conn.close();
            }

        }

        return projectDN;
    }

    @Override
    public ProjectI getProject(String projectName) throws GPMSException {

        if (projectName == null || projectName.isEmpty()) {
            throw new GPMSException("Unable to handle null or empty project name.");
        }

        String projectDN = getProjectDN(projectName);
        return getProjectByDN(projectDN);

    }

    public ProjectI getProjectByDN(String projectDN) throws GPMSException {

        if (projectDN == null || projectDN.isEmpty()) {
            throw new GPMSException("Unable to handle null or empty project DN.");
        }

        ProjectI project = null;
        LDAPConnection conn = null;

        try {
            conn = getConnection();
            SearchResultEntry projEntry = conn.getEntry(projectDN, ATTR_PROJECTCLASS, ATTR_NAME);
            String projectName = projEntry.getAttributeValue(ATTR_NAME);
            ProjectClassI projectClass = getProjectClass(projEntry.getAttributeValue(ATTR_PROJECTCLASS));

            Collection<DataSourceI> projectDataSources = loadDataSources(projectDN);
            project = new Project(projectName, projectClass, projectDataSources, false);
        } catch (LDAPException | ExecutionException ex) {
            throw new GPMSException(ex);
        } finally {
            if (conn != null) {
                conn.close();
            }

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

    @Override
    public MembershipI getService(String projectName, String roleName) throws GPMSException {
        String projectDN = getProjectDN(projectName);
        ProjectI project = getProjectByDN(projectDN);
        for (RoleI role : project.getProjectClass().getRoles()) {
            if (role.getName().equals(roleName)) {
                return new Membership(project, role);
            }
        }
        throw new GPMSException("Cannot obtain service access to project " + projectName);
    }

    private Collection<DataSourceI> loadDataSources(String projectDN) throws LDAPException, GPMSException, ExecutionException {

        List<DataSourceI> datasources = new ArrayList<>();

        LDAPConnection ldapConn = null;
        try {
            ldapConn = getConnection();
            final LDAPConnection conn = ldapConn;

            final String[] dataSourceDNs = conn.getEntry(projectDN, ATTR_DATASOURCE).getAttributeValues(ATTR_DATASOURCE);
            if (dataSourceDNs != null) {

                for (String datasourceDN : dataSourceDNs) {

                    SearchResultEntry dsEntry = conn.getEntry(datasourceDN, ATTR_OBJECTCLASS, ATTR_NAME, ATTR_GPMSHOST, ATTR_DBMSTYPE, ATTR_APITYPE, ATTR_DATASOURCETYPE, ATTR_URL);

                    final String dsName = dsEntry.getAttributeValue(ATTR_NAME);

                    switch (dsEntry.getObjectClassAttribute().getValue()) {
                        case "gpmsDataSourceAS":

                            if (!IGNORE_APPSERV) {
                                //
                                // load datasource type
                                //
                                final String dsTypeDN = dsEntry.getAttributeValue(ATTR_DATASOURCETYPE);
                                DataSourceTypeI dsType = dstype_cache.get(dsTypeDN, new Callable<DataSourceTypeI>() {

                                    @Override
                                    public DataSourceTypeI call() throws Exception {
                                        SearchResultEntry dsTypeEntry = conn.getEntry(dsTypeDN, ATTR_NAME);
                                        return new DataSourceType(dsTypeEntry.getAttributeValue(ATTR_NAME));
                                    }
                                });

                                URI gpmsURL = null;
                                try {
                                    gpmsURL = new URI(dsEntry.getAttributeValue(ATTR_URL));
                                } catch (URISyntaxException ex) {
                                    throw new GPMSException(ex);
                                }

                                DataSource_ApplicationServerI appServDS = new GPMSDataSourceAppServer(dsEntry.getAttributeValue(ATTR_NAME), gpmsURL, dsType);
                                datasources.add(appServDS);
                            }
                            break;
                        case "gpmsDataSourceDB":
                            //
                            // load host
                            //
                            final String hostDN = dsEntry.getAttributeValue(ATTR_GPMSHOST);
                            if (hostDN == null) {
                                throw new GPMSException("gpmsDataSourceDB " + dsName + " has no gpmsHost attribute.");
                            }
                            HostI host = host_cache.get(hostDN, new Callable<HostI>() {

                                @Override
                                public HostI call() throws Exception {
                                    SearchResultEntry hostEntry = conn.getEntry(hostDN, ATTR_HOSTNAME, ATTR_HOSTPORT);
                                    if (hostEntry == null) {
                                        throw new GPMSException("Unable to obtain host " + hostDN);
                                    }
                                    String hostName = hostEntry.getAttributeValue(ATTR_HOSTNAME);
                                    if (hostName == null) {
                                        throw new GPMSException(hostDN + " has no gpmsHostName attribute.");
                                    }
                                    Integer hostPost = hostEntry.getAttributeValueAsInteger(ATTR_HOSTPORT);
                                    if (hostPost == null) {
                                        throw new GPMSException(hostDN + " has no gpmsPort attribute.");
                                    }
                                    return new GPMSHost(hostName, hostPost);
                                }
                            });

                            //
                            // load datasource type
                            //
                            final String dsTypeDN2 = dsEntry.getAttributeValue(ATTR_DATASOURCETYPE);
                            DataSourceTypeI dsType2 = dstype_cache.get(dsTypeDN2, new Callable<DataSourceTypeI>() {

                                @Override
                                public DataSourceTypeI call() throws Exception {
                                    SearchResultEntry dsTypeEntry = conn.getEntry(dsTypeDN2, ATTR_NAME);
                                    return new DataSourceType(dsTypeEntry.getAttributeValue(ATTR_NAME));
                                }
                            });

                            //
                            // load dbmstype
                            //
                            final String dbmsTypeDN = dsEntry.getAttributeValue(ATTR_DBMSTYPE);
                            DBMSTypeI dbms = dbmstype_cache.get(dbmsTypeDN, new Callable<DBMSTypeI>() {

                                @Override
                                public DBMSTypeI call() throws Exception {
                                    SearchResultEntry dbmsTypeEntry = conn.getEntry(dbmsTypeDN, ATTR_NAME, ATTR_VERSION);
                                    return new DBMSType(dbmsTypeEntry.getAttributeValue(ATTR_NAME), dbmsTypeEntry.getAttributeValue(ATTR_VERSION));
                                }
                            });

                            // load API type
                            final String apiTypeDN = dsEntry.getAttributeValue(ATTR_APITYPE);
                            DBAPITypeI apiType = apitype_cache.get(apiTypeDN, new Callable<DBAPITypeI>() {

                                @Override
                                public DBAPITypeI call() throws Exception {
                                    SearchResultEntry apiTypeEntry = conn.getEntry(apiTypeDN, ATTR_NAME, ATTR_VERSION);
                                    return new DBAPIType(apiTypeEntry.getAttributeValue(ATTR_NAME));
                                }
                            });

                            // create datasource
                            DataSource_DBI dataSource = new GPMSDataSourceDB(dsName, dsType2, dbms, apiType, host);
                            datasources.add(dataSource);
                            break;
                        default:
                            throw new GPMSException("Unknown objectClass for DataSource " + datasourceDN);
                    }

                }
            }
        } finally {
            if (ldapConn != null) {
                ldapConn.close();
            }
        }
        return datasources;
    }

    ProjectClassI getProjectClass(String projClassDN) throws GPMSException {
        if (supportedProjectClasses.containsKey(projClassDN)) {
            return supportedProjectClasses.get(projClassDN);
        }

        //log("Loading new project class " + projClassDN.toString());
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            SearchResultEntry pcEntry = conn.getEntry(projClassDN, ATTR_NAME);
            if (pcEntry != null) {
                ProjectClassI pClass = new ProjectClass(pcEntry.getAttributeValue(ATTR_NAME));
                loadRoles(pClass);
                supportedProjectClasses.put(projClassDN, pClass);
                return pClass;
            }

        } catch (LDAPException ex) {
            throw new GPMSException(ex);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        throw new GPMSException("No such project class: " + projClassDN);
    }

    private void loadRoles(ProjectClassI pClass) throws GPMSException {
        if (pClass == null || pClass.getName() == null || pClass.getName().isEmpty()) {
            throw new GPMSException("Unable to load roles for null/empty project class.");
        }

        String cfgFileName = new StringBuilder(config != null ? config.getGPMSConfigDirectory() : "")
                .append(File.separator).append(pClass.getName().toLowerCase()).append(".conf").toString();
        File cfgFile = new File(cfgFileName);
        if (!cfgFile.exists()) {
            log(cfgFile.getAbsolutePath() + " missing or unreadable, obtaining roles for " + pClass.getName() + " from LDAP directory.");
            loadRolesFromDirectory(pClass);
            return;
        }

        //
        // read roles from gpms config file
        //
        log("Reading " + pClass.getName() + " role file " + cfgFile.getAbsolutePath());

        String line;
        boolean in_section = false;
        try ( BufferedReader br = new BufferedReader(new FileReader(cfgFileName))) {
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

//        LDAPConnection conn = null;
//
//        try {
//            conn = getConnection();
//            Filter roleFilter = Filter.create("(objectClass=gpmsRole)");
//            final SearchResult roleResult = conn.search(
//                    new SearchRequest(String.format("name=%s,ou=Project_Class,%s", pClass.getName(), gpmsBaseDN),
//                            SearchScope.SUB, roleFilter, ATTR_NAME));
//            for (SearchResultEntry sre : roleResult.getSearchEntries()) {
//                if (sre.hasAttribute(ATTR_NAME)) {
//                    final String roleName = sre.getAttributeValue(ATTR_NAME);
//                    RoleI role = new Role(pClass, roleName);
//                    pClass.getRoles().add(role);
//                    ret.add(role);
//
//                    dbAccess.put(role, new String[]{"dbUser", "dbPass"});
//                }
//            }
//        } catch (LDAPException ex) {
//            ret.clear();
//            throw new GPMSException(ex);
//        } finally {
//            if (conn != null) {
//                conn.close();
//            }
//        }
    }

    private void loadRolesFromDirectory(ProjectClassI pClass) throws GPMSException {

        LDAPConnection conn = null;

        try {
            conn = getConnection();
            Filter pclassFilter = Filter.create("(objectClass=gpmsRole)");
            final SearchResult roleResult = conn.search(new SearchRequest(String.format("name=%s,%s", pClass.getName(), projClassBaseDN), SearchScope.SUB, pclassFilter, ATTR_NAME));
            for (SearchResultEntry sre : roleResult.getSearchEntries()) {
                String roleName = sre.getAttributeValue(ATTR_NAME);
                log(pClass.getName() + ": found LDAP role: " + roleName);
                RoleI r = new Role(pClass, roleName);
                pClass.getRoles().add(r);
            }
        } catch (LDAPException ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getMessage());
            throw new GPMSException(ex);
        } finally {
            if (conn != null) {
                conn.close();
            }

        }
    }

    private LDAPConnection getConnection() throws LDAPException {
        return ldapPool.getConnection();
    }

    private final static Logger logger = Logger.getLogger(LDAPDataLoader.class.getName());

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
