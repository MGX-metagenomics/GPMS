/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.nodesupport;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.rest.RESTMasterI;
import org.openide.nodes.Node;

/**
 *
 * @author sj
 */
public interface GPMSProjectSupportI {

    public boolean isSupported(MembershipI mbr);

    public Node createProjectNode(RESTMasterI restMaster);
}
