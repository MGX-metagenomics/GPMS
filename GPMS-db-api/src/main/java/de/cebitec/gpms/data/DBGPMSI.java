package de.cebitec.gpms.data;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.GPMSMessageI;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface DBGPMSI {

    public Collection<ProjectClassI> getSupportedProjectClasses();

    public MembershipI getService(String projectName, String roleName) throws GPMSException;

    public <T extends MasterI> void createMaster(MembershipI m, Class<T> targetClass);

    public <T extends MasterI> void createServiceMaster(MembershipI m, Class<T> targetClass);

    public <T extends MasterI> T getCurrentMaster();

    public UserI getCurrentUser();

    public List<GPMSMessageI> getMessages();
}
