/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DataSourceTypeI;
import de.cebitec.gpms.core.DataSource_ApplicationServerI;
import java.net.URI;

/**
 *
 * @author sjaenick
 */
public class GPMSDataSourceAppServer extends GPMSDataSource implements DataSource_ApplicationServerI {

    private final URI uri;
    
    public GPMSDataSourceAppServer(String name, URI uri, DataSourceTypeI dsType) {
        super(name, dsType);
        this.uri = uri;
    }

    @Override
    public URI getURL() {
        return uri;
    }
    
}
