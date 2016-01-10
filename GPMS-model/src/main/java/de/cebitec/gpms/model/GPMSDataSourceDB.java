/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DBAPITypeI;
import de.cebitec.gpms.core.DBMSTypeI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.DataSourceTypeI;
import de.cebitec.gpms.core.HostI;

/**
 *
 * @author sjaenick
 */
public class GPMSDataSourceDB extends GPMSDataSource implements DataSource_DBI {

    private final DBMSTypeI dbmsType;
    private final DBAPITypeI dbApiType;
    private final HostI host;
    
    public GPMSDataSourceDB(String name, DataSourceTypeI dsType, DBMSTypeI dbmsType, DBAPITypeI dbApiType, HostI host) {
        super(name, dsType);
        this.dbmsType = dbmsType;
        this.dbApiType = dbApiType;
        this.host = host;
    }
    
    @Override
    public DBMSTypeI getDBMSType() {
        return dbmsType;
    }

    @Override
    public DBAPITypeI getAPIType() {
        return dbApiType;
    }

    @Override
    public HostI getHost() {
        return host;
    }
    
}
