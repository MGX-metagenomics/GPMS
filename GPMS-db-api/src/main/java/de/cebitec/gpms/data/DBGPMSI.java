package de.cebitec.gpms.data;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public interface DBGPMSI {

//    public Set<ProjectClassI> getProjectClasses();
//
    public Collection<ProjectClassI> getSupportedProjectClasses();

//    public void registerEMFResolver(EMFNameResolver resolver);
//
//    public void unregisterEMFResolver(EMFNameResolver resolver);

//    public void registerProjectClass(String pc);
//
//    public void unregisterProjectClass(String pc);

    public void createMaster(MembershipI m);

    public JPAMasterI getCurrentMaster();

    public UserI getCurrentUser();
}
