/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.data;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MasterI;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface JDBCMasterI extends MasterI {

    public DataSource getDataSource();
    
    public DataSource_DBI getGPMSDatasource();
    
}
