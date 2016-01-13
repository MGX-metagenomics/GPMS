
package de.cebitec.gpms.db;

import de.cebitec.gpms.data.GPMSDataLoaderI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
/**
 *
 * @author sj
 */
public class GPMSProxyDataSource implements ProxyDataSourceI {
    
    private final GPMSDataLoaderI gpmsLoader;

    public GPMSProxyDataSource(GPMSDataLoaderI gpmsLoader) {
        this.gpmsLoader = gpmsLoader;
    }
    
    private DataSource getTarget() {
        return gpmsLoader.getCurrentMaster().getDataSource();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return getTarget().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getTarget().getConnection(username, password);
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
