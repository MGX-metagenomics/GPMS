package de.cebitec.gpms.core;

import java.beans.PropertyChangeListener;

/**
 *
 * @author sjaenick
 */
public interface MasterI extends AutoCloseable {

    public final static String PROP_LOGGEDIN = "master_loggedInState";

    public ProjectI getProject();

    public RoleI getRole();

    public void setUser(UserI user);

    public UserI getUser();

    public void log(String message);

    @Override
    public void close();

    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);

}
