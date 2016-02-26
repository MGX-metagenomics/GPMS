package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
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

    public UserI getUser();

    public String getBaseURI();

    public String getServerName();

    public Iterator<ProjectClassI> getProjectClasses();

    MasterI createMaster(MembershipI m);

    Iterator<MembershipI> getMemberships() throws GPMSException;

    boolean login(String user, String password) throws GPMSException;

    void logout();

    boolean loggedIn();

    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);
}
