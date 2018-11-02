/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.rest.GPMSClientFactory;
import de.cebitec.gpms.rest.GPMSClientI;

/**
 *
 * @author sj
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, GPMSException {
        GPMSClientI cebitec = GPMSClientFactory.createClient("CeBiTec", "https://mgx.cebitec.uni-bielefeld.de/MGX-maven-web/webresources/", true);
        cebitec.login("mgx_unittestRO", "gut-isM5iNt");

        GPMSClientI jlu = GPMSClientFactory.createClient("JLU", "https://mgx.computational.bio.uni-giessen.de/MGX-maven-web/webresources/", true);
        jlu.login("mgx_unittestRO", "gut-isM5iNt");

//        GPMSClient localhost = GPMSClientFactory.createClient("dl560", "https://127.0.0.1:8443/MGX-maven-web/webresources/", false);
//        System.err.println("logging in");
//        localhost.login("mgx_unittestRO", "gut-isM5iNt");
//        System.err.println("logged in");

        while (true) {
            long cebMS = System.currentTimeMillis();
            cebitec.ping();
            cebMS = System.currentTimeMillis() - cebMS;

            long jluMS = System.currentTimeMillis();
            jlu.ping();
            jluMS = System.currentTimeMillis() - jluMS;

            long localMS = System.currentTimeMillis();
//            localhost.ping();
            localMS = System.currentTimeMillis() - localMS;

            System.out.println("CeBiTec " + cebMS + " JLU " + jluMS + " dl560 "+ localMS);

            Thread.sleep(1000);
        }
    }

}
