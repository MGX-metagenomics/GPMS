package de.cebitec.gpms.db;

import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class GPMSProxyDataSource implements ProxyDataSourceI {
    
    private final GPMSDataLoader gpmsLoader;
    
    public GPMSProxyDataSource(GPMSDataLoader gpmsLoader) {
        this.gpmsLoader = gpmsLoader;
    }
    
    private GPMSManagedDataSourceI getTarget() {
        return gpmsLoader.<JDBCMasterI>getCurrentMaster().getDataSource();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return new GPMSProxyConnection(getTarget());
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new GPMSProxyConnection(getTarget(), username, password);
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getTarget().unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getTarget().isWrapperFor(iface);
    }
    
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getTarget().getLogWriter();
    }
    
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getTarget().setLogWriter(out);
    }
    
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getTarget().setLoginTimeout(seconds);
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return getTarget().getLoginTimeout();
    }
    
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getTarget().getParentLogger();
    }
}
