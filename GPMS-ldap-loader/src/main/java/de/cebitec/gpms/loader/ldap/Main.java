/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.loader.ldap;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import java.util.Collection;

/**
 *
 * @author sj
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws GPMSException {
        System.out.println("testGetMemberships");

        LDAPDataLoader l = new LDAPDataLoader();
        l.start();

        long now = System.currentTimeMillis();
        Collection<MembershipI> memberships = l.getMemberships("sjaenick");

//        for (MembershipI mbr : memberships) {
//            System.err.println("   " + mbr.getProject().getName() + " / " + mbr.getRole().getName());
//        }
        now = System.currentTimeMillis() - now;
        System.err.println(now + " ms");

        now = System.currentTimeMillis();
        memberships = l.getMemberships("sjaenick");

//        for (MembershipI mbr : memberships) {
//            System.err.println("   " + mbr.getProject().getName() + " / " + mbr.getRole().getName());
//        }
        now = System.currentTimeMillis() - now;
        System.err.println(now + " ms");

    }
}
