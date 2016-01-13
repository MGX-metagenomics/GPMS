/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.core;

import java.net.URI;

/**
 *
 * @author sjaenick
 */
public interface DataSource_ApplicationServerI extends DataSourceI {
    
    public URI getURL();
    
}
