/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSourceTypeI;

/**
 *
 * @author sjaenick
 */
public class GPMSDataSource implements DataSourceI {

    private final String name;
    private final DataSourceTypeI dsType;

    public GPMSDataSource(String name, DataSourceTypeI dsType) {
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

}
