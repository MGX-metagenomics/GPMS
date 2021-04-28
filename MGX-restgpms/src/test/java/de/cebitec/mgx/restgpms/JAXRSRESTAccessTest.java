///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package de.cebitec.mgx.restgpms;
//
//import de.cebitec.gpms.core.DataSourceI;
//import de.cebitec.gpms.core.DataSource_ApplicationServerI;
//import de.cebitec.gpms.core.GPMSException;
//import de.cebitec.gpms.core.MembershipI;
//import de.cebitec.gpms.rest.AsyncRequestHandleI;
//import de.cebitec.gpms.rest.GPMSClientI;
//import de.cebitec.gpms.rest.RESTMasterI;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
///**
// *
// * @author sj
// */
//public class JAXRSRESTAccessTest {
//
//    GPMSClientI gpms = null;
//    JAXRSRESTAccess access = null;
//
//    @Before
//    public void setUp() throws GPMSException {
//        gpms = TestMaster.get();
//        gpms.login("mgx_unittestRO", "gut-isM5iNt");
//        Iterator<MembershipI> memberships = gpms.getMemberships();
//        while (memberships.hasNext()) {
//            MembershipI m = memberships.next();
//            if (m.getProject().getName().equals("MGX2_Unittest")) {
//                RESTMasterI rest = gpms.createMaster(m); //new RESTMaster(gpms, m, gpms.getUser());
//                DataSource_ApplicationServerI appServer = null;
//                for (DataSourceI rds : rest.getProject().getDataSources()) {
//                    if (rds instanceof DataSource_ApplicationServerI) {
//                        appServer = (DataSource_ApplicationServerI) rds;
//                    }
//                }
//                access = new JAXRSRESTAccess(rest.getUser(), appServer.getURL(), rest.validateSSL());
//            }
//        }
//    }
//
//    @After
//    public void tearDown() throws IOException {
//        access.close();
//        access = null;
//        gpms.logout();
//        gpms = null;
//    }
//
//    @Test
//    public void testGet() throws Exception {
//        System.out.println("get");
//        assertNotNull(access);
//        long duration = System.currentTimeMillis();
//        for (int i = 0; i < 20; i++) {
//            access.get("Sequence", "sleep", "500");
//        }
//        duration = System.currentTimeMillis() - duration;
//        System.out.println(duration + "ms");
//        assertTrue(duration > 10000);
//    }
//
//    @Test
//    public void testGetAsync() throws Exception {
//        System.out.println("getAsync");
//        List<AsyncRequestHandleI> ret = new ArrayList<>();
//
//        long duration = System.currentTimeMillis();
//        for (int i = 0; i < 20; i++) {
//            AsyncRequestHandleI handle = access.getAsync("Sequence", "sleep", "500");
//            ret.add(handle);
//        }
//        for (AsyncRequestHandleI handle : ret) {
//            boolean success = handle.isSuccess();
//            assertTrue(success);
//        }
//        duration = System.currentTimeMillis() - duration;
//        System.out.println(duration + "ms");
//        assertTrue(duration < 1000);
//    }
//
//    @Test
//    public void testClose() throws Exception {
//
//        JAXRSRESTAccess acc = null;
//
//        GPMSClientI gpms = TestMaster.get();
//        gpms.login("mgx_unittestRO", "gut-isM5iNt");
//        Iterator<MembershipI> memberships = gpms.getMemberships();
//        while (memberships.hasNext()) {
//            MembershipI m = memberships.next();
//            if (m.getProject().getName().equals("MGX2_Unittest")) {
//                RESTMasterI rest = new RESTMaster(gpms, m, gpms.getUser());
//                DataSource_ApplicationServerI appServer = null;
//                for (DataSourceI rds : rest.getProject().getDataSources()) {
//                    if (rds instanceof DataSource_ApplicationServerI) {
//                        appServer = (DataSource_ApplicationServerI) rds;
//                    }
//                }
//                acc = new JAXRSRESTAccess(rest.getUser(), appServer.getURL(), rest.validateSSL());
//            }
//        }
//        assertNotNull(acc);
//        assertFalse(acc.isClosed());
//        acc.close();
//        assertTrue(acc.isClosed());
//
//    }
//}
