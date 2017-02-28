/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.HostI;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.host);
        hash = 41 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPMSHost other = (GPMSHost) obj;
        if (this.port != other.port) {
            return false;
        }
        return Objects.equals(this.host, other.host);
    }
}
