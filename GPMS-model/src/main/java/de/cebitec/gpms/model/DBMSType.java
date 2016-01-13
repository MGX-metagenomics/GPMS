/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DBMSTypeI;

/**
 *
 * @author sjaenick
 */
public class DBMSType implements DBMSTypeI {

    private final String name;
    private final String version;

    public DBMSType(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

}
