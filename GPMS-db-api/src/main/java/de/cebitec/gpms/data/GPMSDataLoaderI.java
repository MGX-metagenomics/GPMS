/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.data;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public interface GPMSDataLoaderI {

    public JPAMasterI createMaster(MembershipI mbr);

    public JPAMasterI getCurrentMaster();

    public void setCurrentMaster(JPAMasterI master);

    public Collection<MembershipI> getMemberships(String userLogin) throws GPMSException;

    public Collection<ProjectClassI> getSupportedProjectClasses();

}
