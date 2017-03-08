/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.db.sql;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import de.cebitec.gpms.util.GPMSManagedConnectionI;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.sql.DataSource;

/**
 *
 * @author sj
 */
public class GPMSManagedDataSource implements GPMSManagedDataSourceI {

    private final DataSource_DBI gpmsDS;
    private final DataSource dataSource;
    private final AtomicInteger numSubscribers = new AtomicInteger(0);
    // number of unclosed connections
    private final AtomicInteger connInUse = new AtomicInteger(0);
    //
    private final TObjectIntMap<String> subscribers = new TObjectIntHashMap<>(10, 0.5f, 0);
    //
    private static final Logger LOG = Logger.getLogger(GPMSManagedDataSource.class.getName());

    public GPMSManagedDataSource(DataSource dataSource, DataSource_DBI gpmsDS) {
        this.dataSource = dataSource;
        this.gpmsDS = gpmsDS;
    }

    @Override
    public final String getName() {
        return gpmsDS.getName();
    }

    @Override
    public final void subscribe() {
        numSubscribers.incrementAndGet();

        String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        synchronized (subscribers) {
            if (!subscribers.increment(caller)) {
                subscribers.put(caller, 1);
            } else {
                throw new RuntimeException("Duplicate datasource subscription by " + caller);
            }
        }
//        System.err.println("Datasource#subscribe: " + connInUse.get() + " connections used, " + numSubscribers.get() + " subscriptions.");
    }

    @Override
    public final synchronized void close() {

        String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        synchronized (subscribers) {
            if (!subscribers.containsKey(caller)) {
                System.err.println("DataSource#close invoked by unregistered caller " + caller);
            } else {
                subscribers.adjustValue(caller, -1);
                if (subscribers.get(caller) == 0) {
                    subscribers.remove(caller);
                }
            }
        }

        // close == unsubscribe
        numSubscribers.decrementAndGet();

//        System.err.println("Datasource#close: " + connInUse.get() + " connections used, " + numSubscribers.get() + " subscriptions.");
        if (connInUse.get() == 0 && numSubscribers.get() == 0) {
            LOG.log(Level.INFO, "Closing backend SQL datasource for {0}", gpmsDS.getName());
            if (dataSource != null && dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception ex) {
                }
            }
        } else {
            System.err.println("Not closing datasource, " + connInUse.get() + " connections still used, " + numSubscribers.get() + " subscriptions.");
            subscribers.forEachEntry(new TObjectIntProcedure<String>() {
                @Override
                public boolean execute(String a, int b) {
                    System.err.println("    " + a + ": " + b);
                    return true;
                }

            });
//            assert false;
        }
    }

    @Override
    public final GPMSManagedConnectionI getConnection() throws SQLException {

        String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        synchronized (subscribers) {
            if (!subscribers.containsKey(caller)) {
                System.err.println("DataSource#getConnection invoked by unregistered caller " + caller);
                System.err.println("registered callers are:");
                subscribers.forEachEntry(new TObjectIntProcedure<String>() {
                    @Override
                    public boolean execute(String a, int b) {
                        System.err.println("    " + a + ": " + b);
                        return true;
                    }

                });
                throw new SQLException("DataSource#getConnection invoked by unregistered caller " + caller);
            }
        }

        Connection c = dataSource.getConnection();
        if (c != null) {
            connInUse.incrementAndGet();
//            System.err.println("Datasource#getConnection: " + connInUse.get() + " connections used, " + numSubscribers.get() + " subscriptions.");
            return new GPMSManagedConnection(c);
        }
        return null;
    }

    @Override
    public final GPMSManagedConnectionI getConnection(String username, String password) throws SQLException {

        String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        synchronized (subscribers) {
            if (!subscribers.containsKey(caller)) {
                System.err.println("DataSource#getConnection(user, pass) invoked by unregistered caller " + caller);
                throw new SQLException("DataSource#getConnection invoked by unregistered caller " + caller);
            }
        }

        Connection c = dataSource.getConnection(username, password);
        if (c != null) {
            connInUse.incrementAndGet();
            return new GPMSManagedConnection(c);
        }
        return null;
    }

    @Override
    public final PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public final void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public final void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public final int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public final Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public final <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.gpmsDS);
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPMSManagedDataSource other = (GPMSManagedDataSource) obj;
        return Objects.equals(this.gpmsDS, other.gpmsDS);
    }

    private final class GPMSManagedConnection implements GPMSManagedConnectionI {

        private final Connection conn;

        public GPMSManagedConnection(Connection conn) {
            this.conn = conn;
        }

        @Override
        public final Statement createStatement() throws SQLException {
            return conn.createStatement();
        }

        @Override
        public final PreparedStatement prepareStatement(String sql) throws SQLException {
            return conn.prepareStatement(sql);
        }

        @Override
        public final CallableStatement prepareCall(String sql) throws SQLException {
            return conn.prepareCall(sql);
        }

        @Override
        public final String nativeSQL(String sql) throws SQLException {
            return conn.nativeSQL(sql);
        }

        @Override
        public final void setAutoCommit(boolean autoCommit) throws SQLException {
            conn.setAutoCommit(autoCommit);
        }

        @Override
        public final boolean getAutoCommit() throws SQLException {
            return conn.getAutoCommit();
        }

        @Override
        public final void commit() throws SQLException {
            conn.commit();
        }

        @Override
        public final void rollback() throws SQLException {
            conn.rollback();
        }

        @Override
        public final void close() throws SQLException {
            if (!conn.isClosed()) {
                connInUse.decrementAndGet();
//                System.err.println("Connection#close: " + connInUse.get() + " connections used, " + numSubscribers.get() + " subscriptions.");
                conn.close();
            } else {
                System.err.println("Duplicate Connection#close()");
                assert false;
            }
        }

        @Override
        public final boolean isClosed() throws SQLException {
            return conn.isClosed();
        }

        @Override
        public final DatabaseMetaData getMetaData() throws SQLException {
            return conn.getMetaData();
        }

        @Override
        public final void setReadOnly(boolean readOnly) throws SQLException {
            conn.setReadOnly(readOnly);
        }

        @Override
        public final boolean isReadOnly() throws SQLException {
            return conn.isReadOnly();
        }

        @Override
        public final void setCatalog(String catalog) throws SQLException {
            conn.setCatalog(catalog);
        }

        @Override
        public final String getCatalog() throws SQLException {
            return conn.getCatalog();
        }

        @Override
        public final void setTransactionIsolation(int level) throws SQLException {
            conn.setTransactionIsolation(level);
        }

        @Override
        public final int getTransactionIsolation() throws SQLException {
            return conn.getTransactionIsolation();
        }

        @Override
        public final SQLWarning getWarnings() throws SQLException {
            return conn.getWarnings();
        }

        @Override
        public final void clearWarnings() throws SQLException {
            conn.clearWarnings();
        }

        @Override
        public final Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return conn.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public final Map<String, Class<?>> getTypeMap() throws SQLException {
            return conn.getTypeMap();
        }

        @Override
        public final void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            conn.setTypeMap(map);
        }

        @Override
        public final void setHoldability(int holdability) throws SQLException {
            conn.setHoldability(holdability);
        }

        @Override
        public final int getHoldability() throws SQLException {
            return conn.getHoldability();
        }

        @Override
        public final Savepoint setSavepoint() throws SQLException {
            return conn.setSavepoint();
        }

        @Override
        public final Savepoint setSavepoint(String name) throws SQLException {
            return conn.setSavepoint(name);
        }

        @Override
        public final void rollback(Savepoint savepoint) throws SQLException {
            conn.rollback(savepoint);
        }

        @Override
        public final void releaseSavepoint(Savepoint savepoint) throws SQLException {
            conn.releaseSavepoint(savepoint);
        }

        @Override
        public final Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public final PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return conn.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public final PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return conn.prepareStatement(sql, columnIndexes);
        }

        @Override
        public final PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return conn.prepareStatement(sql, columnNames);
        }

        @Override
        public final Clob createClob() throws SQLException {
            return conn.createClob();
        }

        @Override
        public final Blob createBlob() throws SQLException {
            return conn.createBlob();
        }

        @Override
        public final NClob createNClob() throws SQLException {
            return conn.createNClob();
        }

        @Override
        public final SQLXML createSQLXML() throws SQLException {
            return conn.createSQLXML();
        }

        @Override
        public final boolean isValid(int timeout) throws SQLException {
            return conn.isValid(timeout);
        }

        @Override
        public final void setClientInfo(String name, String value) throws SQLClientInfoException {
            conn.setClientInfo(name, value);
        }

        @Override
        public final void setClientInfo(Properties properties) throws SQLClientInfoException {
            conn.setClientInfo(properties);
        }

        @Override
        public final String getClientInfo(String name) throws SQLException {
            return conn.getClientInfo(name);
        }

        @Override
        public final Properties getClientInfo() throws SQLException {
            return conn.getClientInfo();
        }

        @Override
        public final Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return conn.createArrayOf(typeName, elements);
        }

        @Override
        public final Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return conn.createStruct(typeName, attributes);
        }

        @Override
        public final void setSchema(String schema) throws SQLException {
            conn.setSchema(schema);
        }

        @Override
        public final String getSchema() throws SQLException {
            return conn.getSchema();
        }

        @Override
        public final void abort(Executor executor) throws SQLException {
            conn.abort(executor);
        }

        @Override
        public final void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            conn.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public final int getNetworkTimeout() throws SQLException {
            return conn.getNetworkTimeout();
        }

        @Override
        public final <T> T unwrap(Class<T> iface) throws SQLException {
            return conn.unwrap(iface);
        }

        @Override
        public final boolean isWrapperFor(Class<?> iface) throws SQLException {
            return conn.isWrapperFor(iface);
        }

    }
}
