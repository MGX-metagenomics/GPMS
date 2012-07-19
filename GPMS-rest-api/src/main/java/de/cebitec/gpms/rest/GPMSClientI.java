package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface GPMSClientI {

    public String getBaseURI();

    public List<ProjectClassI> getProjectClasses();

    RESTMasterI createMaster(MembershipI m);

    List<MembershipI> getMemberships();

    boolean login(String user, String password);

    void logout();

}
