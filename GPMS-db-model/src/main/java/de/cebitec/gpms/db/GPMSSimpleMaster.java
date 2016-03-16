package de.cebitec.gpms.db;

import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class GPMSSimpleMaster implements MasterI {

    private final MembershipI membership;
    private UserI user;

    public GPMSSimpleMaster(MembershipI m) {
        this.membership = m;
    }

    @Override
    public void logout() {
        close();
    }

    @Override
    public void close() {
        // nop
    }

    @Override
    public final ProjectI getProject() {
        return membership.getProject();
    }

    @Override
    public final RoleI getRole() {
        return membership.getRole();
    }

    @Override
    public void log(String msg) {
        Logger.getLogger(getClass().getPackage().getName()).log(Level.INFO, msg);
    }

    @Override
    public final void setUser(UserI user) {
        this.user = user;
    }

    @Override
    public final UserI getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.membership);
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
        final GPMSSimpleMaster other = (GPMSSimpleMaster) obj;
        if (!Objects.equals(this.membership, other.membership)) {
            return false;
        }
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

}
