/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.util;

import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author sj
 */
public interface GPMSManagedDataSourceI extends DataSource, AutoCloseable {

    public String getName();

    @Override
    public void close();

    @Override
    public GPMSManagedConnectionI getConnection(String username, String password) throws SQLException;

    @Override
    public GPMSManagedConnectionI getConnection() throws SQLException;

    public void subscribe();

}
