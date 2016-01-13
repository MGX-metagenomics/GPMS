package de.cebitec.gpms.core;

import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface RoleI {

    public String getName();
    
    public ProjectClassI getProjectClass();

    public List<RightI> getRights();
}
