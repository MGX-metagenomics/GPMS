/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DBAPITypeI;
import java.util.Objects;

/**
 *
 * @author sjaenick
 */
public class DBAPIType implements DBAPITypeI {

    private final String apiType;

    public DBAPIType(String apiType) {
        this.apiType = apiType;
    }

    @Override
    public String getName() {
        return apiType;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.apiType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DBAPIType other = (DBAPIType) obj;
        if (!Objects.equals(this.apiType, other.apiType)) {
            return false;
        }
        return true;
    }

    
}
