/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.rest.GPMSClientI;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
public class GPMSTest {

//    @Configuration
//    public static Option[] configuration() {
//        return options(
//                //bootDelegationPackage("javax.annotation"),
//                junitBundles(),
//                mavenBundle("javax.validation", "validation-api", "2.0.1.Final"),
//                mavenBundle("javax.annotation", "javax.annotation-api", "1.3.2"),
//                mavenBundle("com.sun.activation", "javax.activation", "1.2.0"),
//                mavenBundle("javax.xml.bind", "jaxb-api", "2.3.0"),
//                mavenBundle("com.sun.xml.bind", "jaxb-core", "2.3.0"),
//                mavenBundle("com.sun.xml.bind", "jaxb-impl", "2.3.0"),
//                mavenBundle().groupId("de.cebitec.mgx").artifactId("RESTEasy-OSGi").version("2.0"),
//                mavenBundle().groupId("org.javassist").artifactId("javassist").version("3.22.0-CR2"),
//                mavenBundle().groupId("org.jboss.spec.javax.ws.rs").artifactId("jboss-jaxrs-api_2.1_spec").version("1.0.2.Final"),
//                mavenBundle().groupId("com.google.protobuf").artifactId("protobuf-java").version("3.11.0"),
//                mavenBundle().groupId("de.cebitec.gpms").artifactId("GPMS-DTO").version("2.0"),
//                mavenBundle().groupId("de.cebitec.gpms").artifactId("GPMS-core-api").version("2.0"),
//                mavenBundle().groupId("de.cebitec.gpms").artifactId("GPMS-rest-api").version("2.0"),
//                mavenBundle().groupId("de.cebitec.gpms").artifactId("GPMS-model").version("2.0"),
//                mavenBundle().groupId("de.cebitec.mgx").artifactId("ProtoBuf-Serializer").version("2.0"),
//                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
//                bundle("reference:file:target/classes")
//        );
//    }
    @Test
    public void getProjectClassesLoggedOut() {
        System.out.println("getProjectClassesLoggedOut");
        GPMSClientI gpms = TestMaster.get();
        assertNotNull(gpms);
        Iterator<ProjectClassI> projectClasses = null;
        try {
            projectClasses = gpms.getProjectClasses();
        } catch (GPMSException ex) {
            if ("Not logged in.".equals(ex.getMessage())) {
                return;
            }
            fail(ex.getMessage());
        }
        assertNull(projectClasses);
    }

    @Test
    public void testGetProjectClasses() {
        System.out.println("getProjectClasses");
        GPMSClientI gpms = TestMaster.get();
        try {
            gpms.login("mgx_unittestRO", "gut-isM5iNt");
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        Iterator<ProjectClassI> projectClasses = null;
        try {
            projectClasses = gpms.getProjectClasses();
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertNotNull(projectClasses);
        int cnt = 0;
        while (projectClasses.hasNext()) {
            ProjectClassI pc = projectClasses.next();
            assertTrue(pc.getName().startsWith("MGX"));
            Set<RoleI> roles = pc.getRoles();
            for (RoleI role : roles) {
                System.out.println(" " + role.getName());
            }
            assertEquals(3, pc.getRoles().size()); // user, admin, guest
            cnt++;
        }

        gpms.logout();

        assertEquals(2, cnt); // MGX and MGX2
    }

    @Test
    public void testGetMemberships() throws GPMSException {
        System.out.println("getMemberships");
        GPMSClientI gpms = TestMaster.get();
        gpms.login("mgx_unittestRO", "gut-isM5iNt");

        Iterator<MembershipI> memberships = gpms.getMemberships();
        assertNotNull(memberships);
        int cnt = 0;
        DataSourceI restDS = null;
        while (memberships.hasNext()) {
            MembershipI m = memberships.next();
            ProjectI project = m.getProject();
            assertNotNull(project);
            System.out.println(project.getName());
            assertTrue(project.getName().contains("Unittest"));
            assertNotNull(project.getDataSources());
            assertFalse(project.getDataSources().isEmpty());
            for (DataSourceI rds : project.getDataSources()) {
                restDS = rds;
            }
            cnt++;
        }

        gpms.logout();

        assertEquals(2, cnt);

        assertNotNull(restDS);
        //assertNotEquals("", restDS.getURL().toASCIIString());
    }

    @Test
    public void testGetMembershipsRW() throws GPMSException {
        System.out.println("getMembershipsRW");
        GPMSClientI gpms = TestMaster.get();
        gpms.login("mgx_unittestRW", "hL0amo3oLae");
        Iterator<MembershipI> memberships = gpms.getMemberships();
        assertNotNull(memberships);
        int cnt = 0;
        while (memberships.hasNext()) {
            MembershipI m = memberships.next();
            assertNotNull(m.getProject());
            assertNotNull(m.getRole());
            System.err.println("  " + m.getProject().getName());
            assertTrue(m.getProject().getName().endsWith("_Unittest"));
            cnt++;
        }
        assertEquals(2, cnt);
        gpms.logout();
    }

    @Test
    public void testGetMembershipsLoggedOut() throws GPMSException {
        System.out.println("testGetMembershipsLoggedOut");
        GPMSClientI gpms = TestMaster.get();
        Iterator<MembershipI> memberships = gpms.getMemberships();
        assertNotNull(memberships);
        assertFalse(memberships.hasNext());
    }

    @Test
    public void testRESTDataSource() throws GPMSException {
        System.out.println("testRESTDataSource");
        GPMSClientI gpms = TestMaster.get();
        gpms.login("mgx_unittestRO", "gut-isM5iNt");
        Iterator<MembershipI> memberships = gpms.getMemberships();
        MasterI master = null;
        while (memberships.hasNext()) {
            MembershipI m = memberships.next();
            master = gpms.createMaster(m);
            break;
        }
        assertNotNull(master);
        gpms.logout();
    }

    @Test
    public void testLogin() {
        System.out.println("testLogin");
        String login = "mgx_unittestRO";
        String password = "gut-isM5iNt";
        GPMSClientI gpms = TestMaster.get();
        boolean result = false;
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertTrue(result);
        gpms.logout();

    }

    @Test
    public void testLoginPropertyChange() {
        System.out.println("testLoginPropertyChange");
        String login = "mgx_unittestRO";
        String password = "gut-isM5iNt";
        boolean result = false;
        final GPMSClientI gpms = TestMaster.get();
        assertFalse(gpms.loggedIn());

        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            fail(ex.getMessage());

        }
        assertTrue(result);
        assertTrue(gpms.loggedIn());

        gpms.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                assertFalse((Boolean) evt.getNewValue());
                assertFalse(gpms.loggedIn());
            }
        });
        gpms.logout();
        assertFalse(gpms.loggedIn());
    }

    @Test
    public void testLoginGPMSInternal() {
        System.out.println("testLoginGPMSInternal");

        String login = null;
        String password = null;

        String config = System.getProperty("user.home") + "/.m2/mgx.junit";
        File f = new File(config);
        assumeTrue(f.exists() && f.canRead());
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(f));
            login = p.getProperty("username");
            password = p.getProperty("password");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        assumeTrue(login != null);
        assumeTrue(password != null);
        System.out.println("  using credentials for login " + login);

        GPMSClientI gpms = TestMaster.get();
        boolean result = false;
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }

        gpms.logout();

        assertTrue(result);
    }

    @Test
    public void testLoginTwice() {
        System.out.println("testLoginTwice");
        String login = "mgx_unittestRO";
        String password = "gut-isM5iNt";
        GPMSClientI gpms = TestMaster.get();
        assertNotNull(gpms);
        boolean result = false;
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertTrue(result);
        gpms.logout();
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        assertTrue(result);
        gpms.logout();
    }

    @Test
    public void testInvalidLogin() {
        System.out.println("testInvalidLogin");
        String login = "WRONG";
        String password = "WRONG";
        GPMSClientI gpms = TestMaster.get();
        assertNotNull(gpms);
        assertFalse(gpms.loggedIn());

        gpms.logout();

        boolean result = false;
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            if (ex.getMessage().contains("Wrong username/password")) {
            } else {
                fail(ex.getMessage());
            }
        }
        assertFalse(gpms.loggedIn());
    }

    @Test
    public void testInvalidLogin2() {
        System.out.println("testInvalidLogin2");
        GPMSClientI gpms = TestMaster.get();
        assertNotNull(gpms);
        assertFalse(gpms.loggedIn());

        // call login() with wrong credentials on an instance that is already
        // logged in successfully
        String login = "WRONG";
        String password = "WRONG";
        boolean result = false;
        try {
            result = gpms.login(login, password);
        } catch (GPMSException ex) {
            if (ex.getMessage().contains("Wrong username/passw")) {
            } else {
                fail(ex.getMessage());
            }
        }

        assertFalse(gpms.loggedIn());
    }

    @Test
    public void testPing() {
        System.out.println("ping");
        GPMSClientI gpms = TestMaster.get();
        try {
            gpms.login("mgx_unittestRO", "gut-isM5iNt");
        } catch (GPMSException ex) {
            fail(ex.getMessage());
        }
        long result = gpms.ping();
        assertTrue(result > 100000);
        gpms.logout();
        result = gpms.ping();
        assertEquals(-1, result);
    }

    @Test
    public void testCreateMaster() throws GPMSException {
        System.out.println("createMaster");
        GPMSClientI gpms = TestMaster.get();
        gpms.login("mgx_unittestRO", "gut-isM5iNt");
        Iterator<MembershipI> memberships = gpms.getMemberships();
        assertNotNull(memberships);
        int cnt = 0;
        String projNames = "";
        while (memberships.hasNext()) {
            MembershipI m = memberships.next();
            ProjectI project = m.getProject();
            assertNotNull(project);
            projNames += project.getName() + ", ";
            MasterI result = gpms.createMaster(m);
            assertNotNull(result);
            cnt++;
        }
        if (projNames.endsWith(", ")) {
            projNames = projNames.substring(0, projNames.length() - 2);
        }
        assertEquals(2, cnt, "mgx_unittestRO should only be a member of \'MGX_Unittest\', actual project list: " + projNames);
        gpms.logout();
    }

//    @Test
//    public void testGetMembershipsPrivate() throws GPMSException {
//        System.out.println("testGetMembershipsPrivate");
//
//        String login = null;
//        String password = null;
//
//        String config = System.getProperty("user.home") + "/.m2/mgx.junit";
//        File f = new File(config);
//        Assume.assumeTrue(f.exists() && f.canRead());
//        Properties p = new Properties();
//        try {
//            p.load(new FileInputStream(f));
//            login = p.getProperty("username");
//            password = p.getProperty("password");
//        } catch (IOException ex) {
//            System.out.println(ex.getMessage());
//        }
//        Assume.assumeNotNull(login);
//        Assume.assumeNotNull(password);
//        System.out.println("  using credentials for login " + login);
//
//        GPMSClient gpms = TestMaster.get();
//        gpms.login(login, password);
//        Iterator<MembershipI> memberships = gpms.getMemberships();
//        assertNotNull(memberships);
//        int cnt = 0;
//        while (memberships.hasNext()) {
//            MembershipI m = memberships.next();
//            ProjectI project = m.getProject();
//            System.err.println("     got project " + project.getName());
//            cnt++;
//        }
//        assertTrue(cnt > 0);
//    }
//    @Test
//    public void testGetError() {
//        System.out.println("getError");
//        String result = TestMaster.get().getError();
//        assertNull(result);
//    }
}
