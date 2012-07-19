package de.cebitec.gpms.core;

import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface ProjectClassI {

    public String getName();

    public List<? extends RoleI> getRoles();
}
