package de.cebitec.gpms.model;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class ProjectClass implements ProjectClassI {

    private final String pclassname;
    private final Set<RoleI> roleList;
    
    public ProjectClass(String pclassname) {
        this(pclassname, new HashSet<RoleI>(3));
    }

    public ProjectClass(String pclassname, Set<RoleI> roleList) {
        this.pclassname = pclassname;
        this.roleList = roleList;
    }

    @Override
    public final String getName() {
        return pclassname;
    }

    @Override
    public final Set<RoleI> getRoles() {
        return roleList;
    }

    @Override
    public int hashCode() {
        return pclassname.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectClass other = (ProjectClass) obj;
        return Objects.equals(this.pclassname, other.pclassname);
    }

}
