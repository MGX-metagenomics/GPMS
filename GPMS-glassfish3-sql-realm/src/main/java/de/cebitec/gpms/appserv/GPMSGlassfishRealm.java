package de.cebitec.gpms.appserv;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.common.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.sql.DataSource;
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
    public static final String PARAM_DATASOURCE_JNDI = "datasource-jndi";
    public static final String PARAM_DB_USER = "db-user";
    public static final String PARAM_DB_PASSWORD = "db-password";
    private ConnectorRuntime cr;
    private final static String passwordQuery = "CALL authenticate(?, ?)";
    private DataSource gpmsDataSource = null;

    @Override
    protected void init(Properties props) throws BadRealmException, NoSuchRealmException {
        super.init(props);

        String jaasCtx = props.getProperty(AppservRealm.JAAS_CONTEXT_PARAM);
        String dbUser = props.getProperty(PARAM_DB_USER);
        String dbPassword = props.getProperty(PARAM_DB_PASSWORD);
        String dsJndi = props.getProperty(PARAM_DATASOURCE_JNDI);

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

        if (dsJndi == null) {
            String msg = sm.getString("realm.missingprop", PARAM_DATASOURCE_JNDI, "GPMSRealm");
            throw new BadRealmException(msg);
        }

        this.setProperty(AppservRealm.JAAS_CONTEXT_PARAM, jaasCtx);

        if (dbUser != null && dbPassword != null) {
            log.log(Level.FINE, "Setting username and password.");
            this.setProperty(PARAM_DB_USER, dbUser);
            this.setProperty(PARAM_DB_PASSWORD, dbPassword);
        }

        this.setProperty(PARAM_DATASOURCE_JNDI, dsJndi);

        log.log(Level.FINE, "GPMSRealm : {0} = {1}, {2} = {3}, {4} = {5}",
                new String[]{AppservRealm.JAAS_CONTEXT_PARAM, jaasCtx,
                    PARAM_DATASOURCE_JNDI, dsJndi,
                    PARAM_DB_USER, dbUser});

        cr = Util.getDefaultHabitat().getByContract(ConnectorRuntime.class);

        if (gpmsDataSource == null) {
            try {
                gpmsDataSource = (DataSource) cr.lookupNonTxResource(getProperty(PARAM_DATASOURCE_JNDI), false);
            } catch (NamingException ex) {
            }
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
        return authenticate(login, _passwd);
    }

    private final static String[] authResult = new String[]{"gpmsuser"};

    private String[] authenticate(String username, char[] password) {
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
    private boolean isUserValid(String login, char[] password) {
        boolean valid = false;

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(passwordQuery)) {
                statement.setString(1, login);
                statement.setString(2, new String(password));
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt(1) == 1) {
                            valid = true;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            _logger.log(Level.SEVERE, "jdbcrealm.invaliduserreason", new String[]{login, ex.toString()});
            log.log(Level.SEVERE, "Cannot validate user", ex);
        } catch (NamingException ex) {
            _logger.log(Level.SEVERE, "jdbcrealm.invaliduser", login);
            log.log(Level.SEVERE, "Cannot lookup data source", ex);
        }
        return valid;
    }

    /**
     * Return a connection from the properties configured
     *
     * @return a connection
     */
    private Connection getConnection() throws NamingException, SQLException {

        final String dbUser = getProperty(PARAM_DB_USER);
        final String dbPassword = getProperty(PARAM_DB_PASSWORD);

        if (gpmsDataSource == null) {
            gpmsDataSource = (DataSource) cr.lookupNonTxResource(getProperty(PARAM_DATASOURCE_JNDI), false);
        }

        if (dbUser != null && dbPassword != null) {
            return gpmsDataSource.getConnection(dbUser, dbPassword);
        } else {
            return gpmsDataSource.getConnection();
        }
    }

}
