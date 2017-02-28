/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.util;

import de.cebitec.gpms.core.DataSource_DBI;
import javax.sql.DataSource;

/**
 *
 * @author sj
 */
public interface DataSourceProviderI {
    
    public DataSource getDataSource(DataSource_DBI gpms_DS);
    
    public void removeDataSource(DataSource_DBI gpms_DS);
    
    public GPMSManagedDataSourceI registerDataSource(DataSource_DBI gpms_DS, DataSource sqlDS);
}
