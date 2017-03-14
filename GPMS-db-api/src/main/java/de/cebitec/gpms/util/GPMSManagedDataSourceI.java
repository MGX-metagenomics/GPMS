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
public interface GPMSManagedDataSourceI extends DataSource {

    public String getName();

    public void close(Object caller);

//    public GPMSManagedConnectionI getConnection(Object caller, String username, String password) throws SQLException;

    public GPMSManagedConnectionI getConnection(Object caller) throws SQLException;

    public void subscribe(Object caller);

}
