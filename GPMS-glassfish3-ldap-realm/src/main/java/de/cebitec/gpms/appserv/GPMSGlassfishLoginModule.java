package de.cebitec.gpms.appserv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.appserv.security.AppservPasswordLoginModule;
import de.cebitec.gpms.core.UserI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;

/**
 *
 * @author ljelonek
 */
public final class GPMSGlassfishLoginModule extends AppservPasswordLoginModule {

    private static final Logger LOG = Logger.getLogger("de.cebitec.gpms.appserv");
    private static Cache<UserI, String[]> authcache = null;

    public GPMSGlassfishLoginModule() {
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
    protected final void authenticateUser() throws LoginException {
        LOG.log(Level.FINE, "Authenticating user ''{0}''", _username);

//        if (!(_currentRealm instanceof GPMSGlassfishRealm)) {
//            throw new LoginException("Wrong realm. Expected GPMSGlassfishRealm, is '" + _currentRealm.getClass().getName() + "'");
//        }

        GPMSGlassfishRealm gpmsrealm = (GPMSGlassfishRealm) _currentRealm;

        final UserI curUser = new de.cebitec.gpms.model.User(_username, new String(_passwd));

        String[] grpList = authcache.getIfPresent(curUser);
        if (grpList == null) {
            grpList = gpmsrealm.authenticateUser(_username, _passwd);
            if (grpList != null) {
                // only add to cache if successfully authenticated,
                // otherwise a malicious user could fill up our memory
                // with invalid credentials
                authcache.put(curUser, grpList);
            }
        }

        if (grpList == null) {  // JAAS behavior
            throw new LoginException("GPMSRealm: Login Failed with user '" + _username + "'");
        }

        commitUserAuthentication(grpList);
    }

}
