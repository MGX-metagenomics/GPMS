package de.cebitec.gpms.model;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;

/**
 *
 * @author sjaenick
 */
public class Membership implements MembershipI {

    private final ProjectI project;
    private final RoleI role;

    public Membership(ProjectI project, RoleI role) {
        this.project = project;
        this.role = role;
    }

    @Override
    public ProjectI getProject() {
        return project;
    }

    @Override
    public RoleI getRole() {
        return role;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Membership other = (Membership) obj;
        return this.project.getName().equals(other.project.getName()) && this.role.getName().equals(other.role.getName());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.project != null ? this.project.getName().hashCode() : 0);
        hash = 29 * hash + (this.role != null ? this.role.getName().hashCode() : 0);
        return hash;
    }
}
