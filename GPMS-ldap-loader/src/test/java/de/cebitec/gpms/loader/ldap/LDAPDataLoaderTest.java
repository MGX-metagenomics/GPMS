package de.cebitec.gpms.loader.ldap;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.model.ProjectClass;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author sjaenick
 */
//@RunWith(Arquillian.class)
public class LDAPDataLoaderTest {

    private final static LDAPDataLoader l = new LDAPDataLoader();
//    @EJB
//    LDAPDataLoader l;
//    
//    @Deployment
//    public static WebArchive createDeployment() {
//        File[] libs = Maven.resolver()
//                .loadPomFromFile("pom.xml")
//                .importRuntimeAndTestDependencies()
//                .resolve()
//                .withTransitivity()
//                .asFile();
//        
//        WebArchive war = ShrinkWrap.create(WebArchive.class)
//                .addClass(LDAPDataLoader.class)
//                .addClass(GPMSConfiguration.class)
//                .addAsLibraries(libs);
//        return war;
//    }

    @Before
    public void beforeMethod() {
        InetAddress jim;
        Socket socket = new Socket();
        try {
            // resolve host
            jim = InetAddress.getByName("jim.computational.bio.uni-giessen.de");
            Assume.assumeNotNull(jim);

            // connect ldap port
            InetSocketAddress endPoint = new InetSocketAddress(jim, 389);
            Assume.assumeFalse(endPoint.isUnresolved());
            socket.connect(endPoint, 1000);
            Assume.assumeTrue(socket.isConnected());
        } catch (Exception ex) {
            Assume.assumeTrue("Error in LDAP server connection setup, skipping tests..", false);
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
            }
        }
        
        l.start();
    }

    @Test
    public void testLoadRoles() {
        System.out.println("testLoadRoles");
        Collection<RoleI> roles = null;
        try {
            roles = l.loadRoles(new ProjectClass("MGX"));
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertNotNull(roles);
        assertEquals(3, roles.size());
    }

    @Test
    public void testGetMemberships() {
        System.out.println("testGetMemberships");
        Collection<MembershipI> memberships = null;
        try {
            memberships = l.getMemberships("mgx_unittestRO");
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertNotNull(memberships);
        for (MembershipI mbr : memberships) {
            assertNotNull(mbr.getProject());
            assertNotNull(mbr.getRole());
            System.err.println("   " + mbr.getProject().getName() + " / " + mbr.getRole().getName());
        }
    }

    @Test
    public void testFetchAll() {
        System.out.println("testFetchAll");
        Collection<MembershipI> memberships;
        try {
            memberships = l.getMemberships("*");
        } catch (GPMSException ex) {
            return; // ok
        }
        fail("Membership request should not succeed for wildcards");
    }

    @Test
    public void testInvalidLogin() {
        System.out.println("testInvalidLogin");
        Collection<MembershipI> memberships = null;
        try {
            memberships = l.getMemberships("noSuchUser");
        } catch (GPMSException ex) {
            return;
        }
        fail();
    }

    @BeforeClass
    public static void setUp() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestContext.class.getName());
    }
}
