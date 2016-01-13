package de.cebitec.gpms.core;

import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public interface UserI {

    public String getLogin();
    
    public String getPassword();

    public Collection<MembershipI> getMemberships();
}
