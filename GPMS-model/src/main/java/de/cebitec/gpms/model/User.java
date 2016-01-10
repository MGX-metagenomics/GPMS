package de.cebitec.gpms.model;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.UserI;
import java.util.*;

/**
 *
 * @author sjaenick
 */
public class User implements UserI {

    private final String login;
    private final String password;
    private final Collection<MembershipI> memberships;

    public User(String login, String password) {
        this(login, password, null);
    }

    public User(String login, String password, Collection<MembershipI> memberships) {
        this.login = login;
        this.password = password;
        this.memberships = memberships;
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<MembershipI> getMemberships() {
        return memberships;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.login);
        hash = 13 * hash + Objects.hashCode(this.password);
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
        final User other = (User) obj;
        if (!Objects.equals(this.login, other.login)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        return true;
    }

}
