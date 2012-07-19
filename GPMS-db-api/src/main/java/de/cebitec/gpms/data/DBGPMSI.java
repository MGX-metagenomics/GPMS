package de.cebitec.gpms.data;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.util.EMFNameResolver;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public interface DBGPMSI {

    public Set<ProjectClassI> getProjectClasses();

    public Set<ProjectClassI> getSupportedProjectClasses();

    public void registerEMFResolver(EMFNameResolver resolver);

    public void unregisterEMFResolver(EMFNameResolver resolver);

    public void registerProjectClass(String pc);

    public void unregisterProjectClass(String pc);

    public void createMaster(DBMembershipI m);

    public DBMasterI getCurrentMaster();

    public UserI getCurrentUser();
}
