/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.nodesupport;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.rest.RESTMasterI;
import java.util.ArrayList;
import java.util.List;
import org.openide.nodes.Node;

/**
 *
 * @author sj
 */
public class GPMSNodeSupport {

    private final static List<GPMSProjectSupportI> supported = new ArrayList<>();

    public static boolean isSupported(MembershipI mbr) {
        for (GPMSProjectSupportI supp : supported) {
            if (supp.isSupported(mbr)) {
                return true;
            }
        }
        return false;
    }

    public static Node createProjectNode(RESTMasterI restMaster) {
        for (GPMSProjectSupportI supp : supported) {
            Node node = supp.createProjectNode(restMaster);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public static void register(GPMSProjectSupportI supp) {
        supported.add(supp);
    }

    public static void unregister(GPMSProjectSupportI supp) {
        supported.remove(supp);
    }

}
