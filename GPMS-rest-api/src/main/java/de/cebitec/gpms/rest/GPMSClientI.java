package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public interface GPMSClientI {

    public String getBaseURI();
    
    public String getServerName();

    public Iterator<ProjectClassI> getProjectClasses();

    RESTMasterI createMaster(MembershipI m);

    Iterator<MembershipI> getMemberships();

    boolean login(String user, String password);

    void logout();
    
}
