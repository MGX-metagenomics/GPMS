package de.cebitec.gpms.core;

import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface UserI {

    public String getLogin();

    public List<? extends MembershipI> getMemberships(ProjectClassI projClass);
}
