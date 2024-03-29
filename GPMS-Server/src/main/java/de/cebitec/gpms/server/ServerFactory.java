/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.server;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.rest.GPMSClientFactory;
import de.cebitec.gpms.rest.GPMSClientI;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 *
 * @author sj
 */
public class ServerFactory {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public final static String PROP_CHANGED = "serverListChanged";

    private ServerFactory() {
        // let's assume 20 servers will be enough for now..
        for (int i = 0; i < 19; i++) {
            String siteName = NbPreferences.forModule(ServerFactory.class).get("server" + i, null);
            String siteUri = NbPreferences.forModule(ServerFactory.class).get("uri" + i, null);
            if (siteName != null && siteUri != null) {
                GPMSClientI c = null;
                try {
                    c = GPMSClientFactory.createClient(siteName, siteUri, true);
                } catch (GPMSException ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (c != null) {
                    data.add(c);
                }
            }
        }

        // add default site
        if (data.isEmpty()) {
            try {
                //data.add(GPMSClientFactory.createClient("CeBiTec", "https://mgx.cebitec.uni-bielefeld.de/MGX-maven-web/webresources/", true));
                //data.add(GPMSClientFactory.createClient("JLU", "https://mgx.computational.bio.uni-giessen.de/MGX-maven-web/webresources/", true));
                data.add(GPMSClientFactory.createClient("JLU", "https://mgx2.computational.bio.uni-giessen.de/MGX-rest/webresources/", true));
            } catch (GPMSException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        Collections.sort(data, new Comparator<GPMSClientI>() {
            @Override
            public int compare(GPMSClientI o1, GPMSClientI o2) {
                return o1.getServerName().compareTo(o2.getServerName());
            }
        });
    }

    private final static ServerFactory instance = new ServerFactory();
    private final List<GPMSClientI> data = new ArrayList<>();

    public static ServerFactory getDefault() {
        return instance;
    }

    public List<GPMSClientI> getServers() {
        return data;
    }

    public void addServer(GPMSClientI client) {
        if (client != null) {
            List<GPMSClientI> servers = getServers();
            for (GPMSClientI c : servers) {
                if (c.getServerName().equals(client.getServerName())) {
                    throw new RuntimeException("A server named " + client.getServerName() + " is already configured.");
                }
                if (c.getBaseURI().equals(client.getBaseURI())) {
                    throw new RuntimeException("This server is already present as " + c.getServerName() + ".");
                }
            }
            int i = 0;
            while (true) {
                String siteName = NbPreferences.forModule(ServerFactory.class).get("server" + i, null);
                String siteUri = NbPreferences.forModule(ServerFactory.class).get("uri" + i, null);
                if (siteName == null && siteUri == null) {
                    NbPreferences.forModule(ServerFactory.class).put("server" + i, client.getServerName());
                    NbPreferences.forModule(ServerFactory.class).put("uri" + i, client.getBaseURI());
                    data.add(client);
                    pcs.firePropertyChange(PROP_CHANGED, 0, 1);
                    return;
                }
                i++;
            }
        }
    }

    public void removeServer(String serverName) {
        if (serverName != null) {
            for (int i = 0; i < 19; i++) {
                String siteName = NbPreferences.forModule(ServerFactory.class).get("server" + i, null);
                String siteUri = NbPreferences.forModule(ServerFactory.class).get("uri" + i, null);
                if (siteName != null && siteUri != null && siteName.equals(serverName)) {
                    NbPreferences.forModule(ServerFactory.class).remove("server" + i);
                    NbPreferences.forModule(ServerFactory.class).remove("uri" + i);
                    for (GPMSClientI c : data.toArray(new GPMSClientI[]{})) {
                        if (c.getServerName().equals(serverName)) {
                            data.remove(c);
                            break;
                        }
                    }
                    pcs.firePropertyChange(PROP_CHANGED, 0, 1);
                    return;
                }
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

}
