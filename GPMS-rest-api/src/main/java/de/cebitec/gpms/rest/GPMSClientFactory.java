/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.GPMSException;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author sj
 */
public class GPMSClientFactory {

    private static final ServiceLoader<ClientCreatorI> loader = ServiceLoader.<ClientCreatorI>load(ClientCreatorI.class);

    private GPMSClientFactory() {
    }

    @SuppressWarnings("unchecked")
    public static GPMSClientI createClient(String serverName, String baseURI, boolean validateSSL) throws GPMSException {
        if (!OSGiContext.isOSGi()) {
            // fallback to serviceloader
            ClientCreatorI fac = GPMSClientFactory.get();
            if (fac == null) {
                throw new GPMSException("No ClientCreatorI found.");
            }
            return fac.createClient(serverName, baseURI, validateSSL);
        } else {
            BundleContext context = FrameworkUtil.getBundle(GPMSClientFactory.class).getBundleContext();
            ServiceReference<ClientCreatorI> serviceReference = (ServiceReference<ClientCreatorI>) context.getServiceReference(ClientCreatorI.class.getName());
            ClientCreatorI service = context.<ClientCreatorI>getService(serviceReference);
            return service.createClient(serverName, baseURI, validateSSL);
        }
    }

    private static ClientCreatorI get() {
        Iterator<ClientCreatorI> ps = loader.iterator();
        while (ps != null && ps.hasNext()) {
            ClientCreatorI cc = ps.next();
            if (cc != null) {
                return cc;
            }
        }
        return null;
    }
}
