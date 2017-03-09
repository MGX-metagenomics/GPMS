/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.db.sql;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.RoleI;
import java.util.Objects;

/**
 *
 * @author sj
 */
public class CacheKey {
    
    private final DataSource_DBI datasource;
    private final RoleI role;

    public CacheKey(DataSource_DBI datasource, RoleI role) {
        this.datasource = datasource;
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.datasource);
        hash = 71 * hash + Objects.hashCode(this.role);
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
        final CacheKey other = (CacheKey) obj;
        if (!Objects.equals(this.datasource, other.datasource)) {
            return false;
        }
        return Objects.equals(this.role, other.role);
    }
    
}
