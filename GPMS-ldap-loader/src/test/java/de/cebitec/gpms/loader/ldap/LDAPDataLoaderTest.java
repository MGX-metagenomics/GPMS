/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.loader.ldap;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.model.ProjectClass;
import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sjaenick
 */
public class LDAPDataLoaderTest {

    @Test
    public void testLoadRoles() throws Exception {
        System.out.println("testLoadRoles");
        LDAPDataLoader l = new LDAPDataLoader();
        l.start();
        Collection<RoleI> roles = l.loadRoles(new ProjectClass("MGX"));
        assertNotNull(roles);
        assertEquals(3, roles.size());
        for (RoleI role : roles) {
            System.err.println(role.getName());
        }
    }

    @Test
    public void testGetMemberships() throws Exception {
        System.out.println("testGetMemberships");
        LDAPDataLoader l = new LDAPDataLoader();
        l.start();
        Collection<MembershipI> memberships = l.getMemberships("mgx_unittestRO");
        assertNotNull(memberships);
        for (MembershipI mbr : memberships) {
            assertNotNull(mbr.getProject());
            assertNotNull(mbr.getRole());
            System.err.println(mbr.getProject().getName() + " / " + mbr.getRole().getName());
        }
    }

    @Test
    public void testFetchAll()  {
        System.out.println("testFetchAll");
        LDAPDataLoader l = new LDAPDataLoader();
        l.start();
        Collection<MembershipI> memberships;
        try {
            memberships = l.getMemberships("*");
        } catch (GPMSException ex) {
            return; // ok
        }
        fail("Membership request should not succeed for wildcards");
    }

    @Test
    public void testInvalidLogin() throws Exception {
        System.out.println("testInvalidLogin");
        LDAPDataLoader l = new LDAPDataLoader();
        l.start();
        Collection<MembershipI> memberships = l.getMemberships("noSuchUser");
        assertNotNull(memberships);
        assertEquals(0, memberships.size());
    }
}
