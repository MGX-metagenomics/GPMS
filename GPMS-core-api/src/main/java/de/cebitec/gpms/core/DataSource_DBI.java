/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.core;

/**
 *
 * @author sjaenick
 */
public interface DataSource_DBI extends DataSourceI {
    
    public DBMSTypeI getDBMSType();
    
    public DBAPITypeI getAPIType();
    
    public HostI getHost();
    
}
