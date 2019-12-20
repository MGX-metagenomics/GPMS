package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.GPMSMessageI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public interface GPMSClientI {

    public final static String PROP_LOGGEDIN = "gpmsClient_loggedIn";

    public UserI getUser() throws GPMSException;

    public String getBaseURI();

    public String getServerName();
    
    public boolean validateSSL();
    
    public Iterator<GPMSMessageI> getMessages() throws GPMSException;

    public Iterator<ProjectClassI> getProjectClasses() throws GPMSException;

    RESTMasterI createMaster(MembershipI m) throws GPMSException;

    Iterator<MembershipI> getMemberships() throws GPMSException;

    boolean login(String user, String password) throws GPMSException;

    boolean login(String user, char[] password) throws GPMSException;

    void logout();

    boolean loggedIn();

    long ping();

    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);
}
