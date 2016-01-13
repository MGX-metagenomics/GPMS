/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.loader.ldap;

import de.cebitec.gpms.data.GPMSDataLoaderI;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.unboundid.ldap.sdk.DN;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
public class LDAPDataLoader implements GPMSDataLoaderI {

    //
//    @EJB
//    private GPMSConfiguration config;
    private final String baseDN = "dc=computational,dc=bio,dc=uni-giessen,dc=de";
    private final String gpmsBaseDN = "ou=gpms,ou=services," + baseDN;
    //
    private final String userBaseDN = "ou=users,dc=computational,dc=bio,dc=uni-giessen,dc=de";

    //
    private LDAPConnectionPool ldapPool;

    //
    private ProxyDataSourceI proxyDS = null;
    private EntityManagerFactory emf = null;
    private final ThreadLocal<JPAMasterI> currentMaster = new ThreadLocal<>();
    //
    // caches
    //
    private static Cache<String, List<MembershipI>> membership_cache;
    private static Cache<DN, HostI> host_cache;
    private static Cache<DN, DataSourceTypeI> dstype_cache;
    private static Cache<DN, DBMSTypeI> dbmstype_cache;
    //
    //
    //
    private static final ConcurrentMap<RoleI, String[]> dbAccess = new ConcurrentHashMap<>(5);
    //
    private static final ConcurrentMap<DN, ProjectClassI> supportedProjectClasses = new ConcurrentHashMap<>(5);

    //
    // LDAP attributes
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
    private final static String ATTR_DATASOURCETYPE = "gpmsDataSourceType";
    private final static String ATTR_DATASOURCE = "gpmsDataSource";
    private final static String ATTR_VERSION = "gpmsVersion";
    //
    //
    private final static DBAPITypeI MGX_DBAPI_TYPE = new DBAPIType("MGX");
    private final static DBAPITypeI REST_DBAPI_TYPE = new DBAPIType("REST");

    private final static DataSourceTypeI MGX_DS_TYPE = new DataSourceType("MGX");
    private static DN MGX_DSTYPE_DN;

    @PostConstruct
    public void start() {
        // setup ldap connection pool
        RoundRobinServerSet serverSet = new RoundRobinServerSet(
                new String[]{
                    "localhost"//,
                //"jim.computational.bio.uni-giessen.de"
                }, new int[]{10389});
        try {
            ldapPool = new LDAPConnectionPool(serverSet, null, 10); // unauthenticated connection
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
        host_cache = CacheBuilder.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build();

        // preload MGX data
        try {
            DN pClassDN = new DN("name=MGX,ou=Project_Class,ou=gpms,ou=services,dc=computational,dc=bio,dc=uni-giessen,dc=de");
            getProjectClass(pClassDN);
        } catch (LDAPException | GPMSException ex) {
            Logger.getLogger(LDAPDataLoader.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

//        // publish datasource for JPA in JNDI
//        proxyDS = new GPMSProxyDataSource(this);
//        try {
//            Context ctx = new InitialContext();
//            ctx.rebind(ProxyDataSourceI.JNDI_NAME, proxyDS);
//        } catch (NamingException ex) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
//        }
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
            ReadOnlySearchRequest userReq = new SearchRequest(userBaseDN, SearchScope.SUB, userFilter, "dn");
            final SearchResult userResult = conn.search(userReq);
            if (userResult.getEntryCount() != 1) {
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
                    final DN projectDN = sre.getParentDN();


                    SearchResultEntry projEntry = conn.getEntry(projectDN.toString(), ATTR_NAME, ATTR_PROJECTCLASS);
                    String projectName = projEntry.getAttributeValue(ATTR_NAME);

                    ProjectClassI projectClass = getProjectClass(new DN(projEntry.getAttributeValue(ATTR_PROJECTCLASS)));

                    Collection<DataSourceI> projectDataSources = loadDataSources(projectDN);
                    ProjectI project = new Project(projectName, projectClass, projectDataSources, false);

                    RoleI targetRole = null;
                    final DN roleDN = new DN(sre.getAttributeValue(ATTR_GPMSROLE));
                    String roleName = conn.getEntry(roleDN.toString(), ATTR_NAME).getAttributeValue(ATTR_NAME);
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
        } catch (LDAPException | GPMSException | ExecutionException ex) {
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

    private String[] getDatabaseCredentials(RoleI role) {
        return dbAccess.get(role);
    }

    private Collection<DataSourceI> loadDataSources(DN projectDN) throws LDAPException, GPMSException, ExecutionException {

        List<DataSourceI> datasources = new ArrayList<>();

        LDAPConnection ldapConn = null;
        try {
            ldapConn = getConnection();
            final LDAPConnection conn = ldapConn;

            //log("loading datasources for " + projectDN);
            final String[] dataSourceDNs = conn.getEntry(projectDN.toString(), ATTR_DATASOURCE).getAttributeValues(ATTR_DATASOURCE);
            if (dataSourceDNs != null) {

                for (String datasourceDN : dataSourceDNs) {

                    //log("Retrieving Datasource " + datasourceDN);
                    SearchResultEntry dsEntry = conn.getEntry(datasourceDN, ATTR_OBJECTCLASS, ATTR_NAME, ATTR_GPMSHOST, ATTR_DBMSTYPE, ATTR_DATASOURCETYPE);

                    final String dsName = dsEntry.getAttributeValue(ATTR_NAME);

                    switch (dsEntry.getObjectClassAttribute().getValue()) {
                        case "gpmsDataSourceAS":
                            break;
                        case "gpmsDataSourceDB":
                            //
                            // load host
                            //
                            final DN hostDN = new DN(dsEntry.getAttributeValue(ATTR_GPMSHOST));
                            HostI host = host_cache.get(hostDN, new Callable<HostI>() {

                                @Override
                                public HostI call() throws Exception {
                                    SearchResultEntry hostEntry = conn.getEntry(hostDN.toString(), ATTR_HOSTNAME, ATTR_HOSTPORT);
                                    return new GPMSHost(hostEntry.getAttributeValue(ATTR_HOSTNAME), hostEntry.getAttributeValueAsInteger(ATTR_HOSTPORT));
                                }
                            });

                            //
                            // load datasource type
                            //
                            final DN dsTypeDN = new DN(dsEntry.getAttributeValue(ATTR_DATASOURCETYPE));
                            DataSourceTypeI dsType = dstype_cache.get(dsTypeDN, new Callable<DataSourceTypeI>() {

                                @Override
                                public DataSourceTypeI call() throws Exception {
                                    SearchResultEntry dsTypeEntry = conn.getEntry(dsTypeDN.toString(), ATTR_NAME);
                                    return new DataSourceType(dsTypeEntry.getAttributeValue(ATTR_NAME));
                                }
                            });

                            //
                            // load dbmstype
                            //
                            final DN dbmsTypeDN = new DN(dsEntry.getAttributeValue(ATTR_DBMSTYPE));
                            DBMSTypeI dbms = dbmstype_cache.get(dbmsTypeDN, new Callable<DBMSTypeI>() {

                                @Override
                                public DBMSTypeI call() throws Exception {
                                    SearchResultEntry dbmsTypeEntry = conn.getEntry(dbmsTypeDN.toString(), ATTR_NAME, ATTR_VERSION);
                                    return new DBMSType(dbmsTypeEntry.getAttributeValue(ATTR_NAME), dbmsTypeEntry.getAttributeValue(ATTR_VERSION));
                                }
                            });

                            // create datasource
                            DataSource_DBI dataSource = new GPMSDataSourceDB(dsName, dsType, dbms, MGX_DBAPI_TYPE, host);
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

    ProjectClassI getProjectClass(DN projClassDN) throws GPMSException {
        if (supportedProjectClasses.containsKey(projClassDN)) {
            return supportedProjectClasses.get(projClassDN);
        }

        //log("Loading new project class " + projClassDN.toString());

        LDAPConnection conn = null;
        try {
            conn = getConnection();
            SearchResultEntry pcEntry = conn.getEntry(projClassDN.toString(), ATTR_NAME);
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
        throw new GPMSException("No such project class: " + projClassDN.toString());
    }

    Collection<RoleI> loadRoles(ProjectClassI pClass) throws GPMSException {

        List<RoleI> ret = new ArrayList<>(3);

        LDAPConnection conn = null;

        try {
            conn = getConnection();
            Filter roleFilter = Filter.create("(objectClass=gpmsRole)");
            final SearchResult roleResult = conn.search(
                    new SearchRequest(String.format("name=%s,ou=Project_Class,%s", pClass.getName(), gpmsBaseDN),
                            SearchScope.SUB, roleFilter, ATTR_NAME));
            for (SearchResultEntry sre : roleResult.getSearchEntries()) {
                if (sre.hasAttribute(ATTR_NAME)) {
                    final String roleName = sre.getAttributeValue(ATTR_NAME);
                    RoleI role = new Role(pClass, roleName);
                    pClass.getRoles().add(role);
                    ret.add(role);

                    dbAccess.put(role, new String[]{"dbUser", "dbPass"});
                }
            }
        } catch (LDAPException ex) {
            ret.clear();
            throw new GPMSException(ex);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }

        return ret;
    }

    private LDAPConnection getConnection() throws LDAPException {
        return ldapPool.getConnection();
    }

    private final static Logger logger = Logger.getLogger(LDAPDataLoader.class
            .getName());

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
