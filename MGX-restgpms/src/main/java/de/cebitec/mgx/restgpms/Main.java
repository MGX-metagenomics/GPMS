/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.core.GPMSException;

/**
 *
 * @author sj
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, GPMSException {
        GPMSClient cebitec = new GPMSClient("CeBiTec", "https://mgx.cebitec.uni-bielefeld.de/MGX-maven-web/webresources/", true);
        cebitec.login("mgx_unittestRO", "gut-isM5iNt");
        
        GPMSClient jlu = new GPMSClient("CeBiTec", "https://mgx.computational.bio.uni-giessen.de/MGX-maven-web/webresources/", true);
        jlu.login("mgx_unittestRO", "gut-isM5iNt");

        while (true) {
            long cebMS = System.currentTimeMillis();
            cebitec.ping();
            cebMS = System.currentTimeMillis() - cebMS;

            long jluMS = System.currentTimeMillis();
            jlu.ping();
            jluMS = System.currentTimeMillis() - jluMS;

            System.out.println("CeBiTec " + cebMS + " JLU " + jluMS);

            Thread.sleep(1000);
        }
    }

}
