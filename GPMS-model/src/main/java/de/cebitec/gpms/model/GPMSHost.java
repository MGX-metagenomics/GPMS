/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.HostI;

/**
 *
 * @author sjaenick
 */
public class GPMSHost implements HostI {

    private final String host;
    private final int port;

    public GPMSHost(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHostName() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

}
