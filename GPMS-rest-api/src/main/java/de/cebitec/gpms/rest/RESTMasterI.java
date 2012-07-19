package de.cebitec.gpms.rest;

import com.sun.jersey.api.client.Client;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.UserI;

/**
 *
 * @author sjaenick
 */
public interface RESTMasterI extends MasterI {

    public void registerSerializer(Class c);

    public Client getClient();

    public UserI getUser();

}
