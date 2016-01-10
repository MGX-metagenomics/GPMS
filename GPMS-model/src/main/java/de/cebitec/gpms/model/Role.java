package de.cebitec.gpms.model;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RightI;
import de.cebitec.gpms.core.RoleI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author sjaenick
 */
public class Role implements RoleI {

    private final String rolename;
    private final ProjectClassI pClass;

    public Role(ProjectClassI pClass, String name) {
        this.pClass = pClass;
        this.rolename = name;
    }

    @Override
    public String getName() {
        return rolename;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.rolename);
        hash = 53 * hash + Objects.hashCode(this.pClass.getName());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Role other = (Role) obj;
        if (!Objects.equals(this.rolename, other.rolename)) {
            return false;
        }
        if (!Objects.equals(this.pClass.getName(), other.pClass.getName())) {
            return false;
        }
        return true;
    }



    @Override
    public List<RightI> getRights() {
        // rights are not used within MGX
        return new ArrayList<>();
    }

    @Override
    public ProjectClassI getProjectClass() {
        return pClass;
    }
}
