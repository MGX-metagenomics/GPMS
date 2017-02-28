/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSourceTypeI;
import java.util.Objects;

/**
 *
 * @author sjaenick
 */
public class GPMSDataSource implements DataSourceI {

    protected final String name;
    protected final DataSourceTypeI dsType;

    public GPMSDataSource(String name, DataSourceTypeI dsType) {
        if (name == null) {
            throw new IllegalArgumentException("No name supplied for datasource");
        }
        if (dsType == null) {
            throw new IllegalArgumentException("Null data source type supplied for data source " + name);
        }
        this.name = name;
        this.dsType = dsType;
    }

    @Override
    public DataSourceTypeI getType() {
        return dsType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.dsType);
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
        final GPMSDataSource other = (GPMSDataSource) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.dsType, other.dsType);
    }
}
