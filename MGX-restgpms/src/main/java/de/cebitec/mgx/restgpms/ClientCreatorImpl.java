/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.ClientCreatorI;
import de.cebitec.gpms.rest.GPMSClientI;

/**
 *
 * @author sj
 */
public class ClientCreatorImpl implements ClientCreatorI {

    public ClientCreatorImpl() {
    }

//    @Override
//    public GPMSClientI createClient(String name, String baseURI) {
//        return new GPMSClient(name, baseURI);
//    }

    @Override
    public GPMSClientI createClient(String name, String baseURI, boolean validateSSL) {
        return new GPMSClient(name, baseURI, validateSSL);
    }
}
