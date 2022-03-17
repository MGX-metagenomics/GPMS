package de.cebitec.gpms.appserv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import de.cebitec.gpms.core.UserI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.hk2.annotations.Service;

/**
 * The GPMSGlassfishRealm is a realm-implementation for the glassfish
 * application server. It acts as a connector to the general project management
 * system (GPMS). It defines one group: "gpmsuser" to which everyone who
 * authenticates sucessfully belongs.
 *
 * @author ljelonek
 */
@Service(name = "GPMSRealm")
public class GPMSGlassfishRealm extends AppservRealm {

    private static final Logger log = Logger.getLogger("de.cebitec.gpms.appserv");
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "gpms";
    //
    public static final String PARAM_DIRURL = "directory";
    public static final String PARAM_BASEDN = "base-dn";
    public static final String PARAM_SEARCH_FILTER = "search-filter";
    public static final String PARAM_POOLSIZE = "pool-size";
    //
    private LDAPConnectionPool ldapPool;
    //
    private static Cache<UserI, String[]> authcache = null;

    @Override
    protected void init(Properties props) throws BadRealmException, NoSuchRealmException {
        super.init(props);

        String jaasCtx = props.getProperty(AppservRealm.JAAS_CONTEXT_PARAM);
        String directory = props.getProperty(PARAM_DIRURL);
        String basedn = props.getProperty(PARAM_BASEDN);
        String filter = props.getProperty(PARAM_SEARCH_FILTER);
        String poolSize = props.getProperty(PARAM_POOLSIZE);

        /*
         * Set the jaas context, otherwise server doesn't indentify the login module.
         * jaas-context is the property specified in domain.xml and
         * is the name corresponding to LoginModule
         * config/login.conf
         */
        this.setProperty(AppservRealm.JAAS_CONTEXT_PARAM, jaasCtx);

        if (jaasCtx == null) {
            String msg = sm.getString("realm.missingprop", AppservRealm.JAAS_CONTEXT_PARAM, "GPMSRealm");
            throw new BadRealmException(msg);
        }

        if (directory == null) {
            String msg = sm.getString("realm.missingprop", PARAM_DIRURL, "GPMSRealm");
            throw new BadRealmException(msg);
        }
        if (basedn == null) {
            String msg = sm.getString("realm.missingprop", PARAM_BASEDN, "GPMSRealm");
            throw new BadRealmException(msg);
        }
        if (filter == null) {
            String msg = sm.getString("realm.missingprop", PARAM_SEARCH_FILTER, "GPMSRealm");
            throw new BadRealmException(msg);
        }
        if (poolSize == null) {
            String msg = sm.getString("realm.missingprop", PARAM_POOLSIZE, "GPMSRealm");
            throw new BadRealmException(msg);
        }

        int numConnections;
        try {
            numConnections = Integer.parseInt(poolSize);
        } catch (NumberFormatException nfe) {
            String msg = sm.getString("realm.missingprop", PARAM_POOLSIZE, "GPMSRealm");
            throw new BadRealmException(msg);
        }

        this.setProperty(AppservRealm.JAAS_CONTEXT_PARAM, jaasCtx);
        this.setProperty(PARAM_DIRURL, directory);
        this.setProperty(PARAM_BASEDN, basedn);
        this.setProperty(PARAM_SEARCH_FILTER, filter);
        this.setProperty(PARAM_POOLSIZE, poolSize);

        String[] servers = directory.split(" ");

        String[] hosts = new String[servers.length];
        int[] ports = new int[servers.length];

        for (int i = 0; i < servers.length; i++) {
            String curServer = servers[i];
            int port = 0;
            if (curServer.startsWith("ldaps://")) {
                curServer = curServer.replaceFirst("ldaps://", "");
                port = 636;
            }
            if (curServer.startsWith("ldap://")) {
                curServer = curServer.replaceFirst("ldap://", "");
                port = 389;
            }

            // attempt to parse port after final ':'
            if (curServer.contains(":")) {
                String[] components = curServer.split(":");
                try {
                    port = Integer.parseInt(components[components.length - 1]);
                    curServer = components[0];
                } catch (NumberFormatException nfe) {
                }
            }

            //fallback to default port
            if (port == 0) {
                port = 389;
            }
            hosts[i] = curServer;
            ports[i] = port;
        }

        if (hosts == null || hosts.length == 0) {
            throw new BadRealmException("Empty host list");
        }

        ServerSet serverSet = new RoundRobinServerSet(hosts, ports);
        try {
            BindRequest breq = new SimpleBindRequest("cn=gpms_access,dc=computational,dc=bio,dc=uni-giessen,dc=de", "gpms");
            ldapPool = new LDAPConnectionPool(serverSet, breq, numConnections);
        } catch (LDAPException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        }

        if (authcache == null) {
            //
            // never keep entries for more than 10 minutes
            // so a user can re-gain access e.g. after changing his/her password
            //
            authcache = CacheBuilder.newBuilder()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();
        }
    }

    @Override
    public final String getAuthType() {
        return "gpms";
    }

    private final static Enumeration grpNames = Collections.enumeration(Arrays.asList("gpmsuser"));

    @Override
    public Enumeration getGroupNames(String username) throws InvalidOperationException, NoSuchUserException {
        return grpNames;
    }

    @Override
    public Enumeration getGroupNames() throws BadRealmException {
        return grpNames;
    }

    final String[] authenticateUser(String login, char[] _passwd) {

        final UserI curUser = new de.cebitec.gpms.model.User(login, new String(_passwd));

        String[] ret = authcache.getIfPresent(curUser);
        if (ret != null) {
            return ret; // cache hit
        } else {
            ret = authenticate(login, String.valueOf(_passwd));
            if (ret != null) {
                // only add to cache if successfully authenticated,
                // otherwise a malicious user could fill up our memory
                // with invalid credentials
                authcache.put(curUser, ret);
            }
        }
        return ret;
    }

    private final static String[] authResult = new String[]{"gpmsuser"};

    private String[] authenticate(String username, String password) {
        boolean authenticated = isUserValid(username, password);
        if (authenticated) {
            return authResult;
        } else {
            return null;
        }
    }

    /**
     * Test if a user is valid
     *
     * @param user user's identifier
     * @param password user's password
     * @return true if valid
     */
    private boolean isUserValid(String login, String password) {

        String filter = this.getProperty(PARAM_SEARCH_FILTER);
        filter = filter.replaceAll("%s", login);

        String userDN;
        try {
            Filter userFilter = Filter.create(filter);
            ReadOnlySearchRequest userReq = new SearchRequest(this.getProperty(PARAM_BASEDN), SearchScope.SUB, userFilter, "dn");
            final SearchResult userResult = ldapPool.search(userReq);
            if (userResult.getEntryCount() == 0) {
                return false; // invalid login
            } else if (userResult.getEntryCount() > 1) {
                log.log(Level.SEVERE, "Unexpected number of results: {0} for login {1}", new Object[]{userResult.getEntryCount(), login});
                return false;
            }
            userDN = userResult.getSearchEntries().get(0).getDN();
        } catch (LDAPException ex) {
            log.log(Level.SEVERE, null, ex);
            return false;
        }

        if (userDN == null || userDN.isEmpty()) {
            return false;
        }

        try {
            BindResult result = ldapPool.bindAndRevertAuthentication(userDN, password);
            return result.getResultCode().equals(ResultCode.SUCCESS);
        } catch (LDAPException ex) {
            return false;
        }
    }

}
