package de.cebitec.gpms.core;

/**
 *
 * @author sjaenick
 */
public interface MasterI extends AutoCloseable {

    public ProjectI getProject();

    public RoleI getRole();

    public void setUser(UserI user);

    public UserI getUser();

    public void log(String message);

    @Override
    public void close();

}
