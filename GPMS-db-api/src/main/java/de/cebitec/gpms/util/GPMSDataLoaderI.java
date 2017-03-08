/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.util;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public interface GPMSDataLoaderI {

    <T extends MasterI> T createMaster(MembershipI mbr, Class<T> masterClass) throws GPMSException;

    <T extends MasterI> T getCurrentMaster();

    String[] getDatabaseCredentials(RoleI role) throws GPMSException ;

    Collection<MembershipI> getMemberships(String userLogin) throws GPMSException;
    
    ProjectI getProject(String projectName) throws GPMSException;

    Collection<ProjectClassI> getSupportedProjectClasses();

    void setCurrentMaster(MasterI master);
    
}
