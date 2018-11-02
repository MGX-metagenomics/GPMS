
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.ClientCreatorI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author sj
 */
public class Activator implements BundleActivator {

    ServiceRegistration<?> registerService;

    @Override
    public void start(BundleContext context) throws Exception {
        registerService = context.registerService(ClientCreatorI.class.getName(), new ClientCreatorImpl(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.ungetService(registerService.getReference());
    }

}
