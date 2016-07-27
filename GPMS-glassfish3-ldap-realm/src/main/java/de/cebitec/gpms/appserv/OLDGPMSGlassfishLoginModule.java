//package de.cebitec.gpms.appserv;
//
//import com.sun.appserv.security.AppservPasswordLoginModule;
//import com.sun.enterprise.security.auth.realm.ldap.LDAPRealm;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.security.auth.login.LoginException;
//
///**
// *
// * @author ljelonek
// */
//public class OLDGPMSGlassfishLoginModule extends AppservPasswordLoginModule {
////extends com.sun.enterprise.security.auth.login.PasswordLoginModule {
//
//    private static final Logger log = Logger.getLogger("de.cebitec.gpms.appserv");
//
//    public OLDGPMSGlassfishLoginModule() {
//    }
//    com.sun.enterprise.security.auth.login.PasswordLoginModule xx;
//    private LDAPRealm _ldapRealm;
//
//    @Override
//    protected void authenticateUser() throws LoginException {
//        log.log(Level.FINE, "Authenticating user ''{0}''", _username);
//
//        if (!(_currentRealm instanceof GPMSGlassfishRealm)) {
//            String msg = sm.getString("ldaplm.badrealm");
//            throw new LoginException(msg);
//        }
//        _ldapRealm = (LDAPRealm) _currentRealm;
//
//        // enforce that password cannot be empty.
//        // ldap may grant login on empty password!
//        if (getPasswordChar() == null || getPasswordChar().length == 0) {
//            String msg = sm.getString("ldaplm.emptypassword", _username);
//            throw new LoginException(msg);
//        }
//
//        String mode = _currentRealm.getProperty(LDAPRealm.PARAM_MODE);
//
//        if (LDAPRealm.MODE_FIND_BIND.equals(mode)) {
//            String[] grpList = _ldapRealm.findAndBind(_username, getPasswordChar());
//            commitAuthentication(_username, getPasswordChar(),
//                    _currentRealm, grpList);
//        } else {
//            String msg = sm.getString("ldaplm.badmode", mode);
//            throw new LoginException(msg);
//        }
//
//    }
//
////    /**
////     * Performs authentication for the current user.
////     *
////     */
////    @Override
////    protected void authenticate() throws LoginException {
////        if (!(_currentRealm instanceof LDAPRealm)) {
////            String msg = sm.getString("ldaplm.badrealm");
////            throw new LoginException(msg);
////        }
////        _ldapRealm = (LDAPRealm) _currentRealm;
////
////        // enforce that password cannot be empty.
////        // ldap may grant login on empty password!
////        if (getPasswordChar() == null || getPasswordChar().length == 0) {
////            String msg = sm.getString("ldaplm.emptypassword", _username);
////            throw new LoginException(msg);
////        }
////
////        String mode = _currentRealm.getProperty(LDAPRealm.PARAM_MODE);
////
////        if (LDAPRealm.MODE_FIND_BIND.equals(mode)) {
////            String[] grpList = _ldapRealm.findAndBind(_username, getPasswordChar());
////            commitAuthentication(_username, getPasswordChar(),
////                    _currentRealm, grpList);
////        } else {
////            String msg = sm.getString("ldaplm.badmode", mode);
////            throw new LoginException(msg);
////        }
////
////    }
//}
