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
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 7;
        // attrs from base class
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.dsType);
        //
        hash = 41 * hash + Objects.hashCode(this.dbmsType);
        hash = 41 * hash + Objects.hashCode(this.dbApiType);
        hash = 41 * hash + Objects.hashCode(this.host);
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
        final GPMSDataSourceDB other = (GPMSDataSourceDB) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.dsType, other.dsType)) {
            return false;
        }
        if (!Objects.equals(this.dbmsType, other.dbmsType)) {
            return false;
        }
        if (!Objects.equals(this.dbApiType, other.dbApiType)) {
            return false;
        }
        return Objects.equals(this.host, other.host);
    }

}
