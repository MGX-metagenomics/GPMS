package de.cebitec.gpms.appserv;

import com.sun.appserv.security.AppservPasswordLoginModule;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;

/**
 *
 * @author ljelonek
 */
public class GPMSGlassfishLoginModule extends AppservPasswordLoginModule {

    private static final Logger log = Logger.getLogger("de.cebitec.gpms.appserv");

    public GPMSGlassfishLoginModule() {
    }

    @Override
    protected void authenticateUser() throws LoginException {
        log.log(Level.FINE, "Authenticating user ''{0}''", _username);
        
        if (!(_currentRealm instanceof GPMSGlassfishRealm)) {
            throw new LoginException("Wrong realm. Expected GPMSGlassfishRealm, is '" + _currentRealm.getClass().getName() + "'");
        }

        GPMSGlassfishRealm gpmsrealm = (GPMSGlassfishRealm) _currentRealm;

        String[] grpList = gpmsrealm.authenticateUser(_username, _passwd);

        if (grpList == null) {  // JAAS behavior
            throw new LoginException("GPMSRealm: Login Failed with user '" + _username + "'");
        }
     
        commitUserAuthentication(grpList);
    }

}
