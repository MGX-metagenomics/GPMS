package de.cebitec.gpms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MembershipI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class DataSourceFactory {

    public static HikariDataSource createDataSource(MembershipI m, DataSource_DBI projectGPMSDS, String dbUser, String dbPassword) {

        if (dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("dbUser and dbPassword must not be null.");
        }

        String poolname = new StringBuilder("DS-")
                .append(m.getProject().getName())
                .append("-")
                .append(m.getRole().getName())
                .toString();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName(poolname);
        //cfg.setMinimumPoolSize(5);
        cfg.setMaximumPoolSize(20);
        cfg.setMinimumIdle(2);
        cfg.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        cfg.addDataSourceProperty("user", dbUser);
        cfg.addDataSourceProperty("password", dbPassword);
        cfg.addDataSourceProperty("serverName", projectGPMSDS.getHost().getHostName());
        cfg.addDataSourceProperty("portNumber", projectGPMSDS.getHost().getPort());
        cfg.addDataSourceProperty("databaseName", projectGPMSDS.getName());
        cfg.setConnectionTimeout(1500); // ms
        cfg.setMaxLifetime(1000 * 60 * 2);  // 2 mins
        cfg.setIdleTimeout(1000 * 60);
        cfg.setLeakDetectionThreshold(60000); // 60 sec before in-use connection is considered leaked

        return new HikariDataSource(cfg);
    }

    public static Connection createConnection(DataSource_DBI projectGPMSDS, String dbUser, String dbPassword) throws SQLException {
        String url = new StringBuilder("jdbc:")
                .append(projectGPMSDS.getDBMSType().getName().toLowerCase())
                .append("://")
                .append(projectGPMSDS.getHost().getHostName())
                .append(":")
                .append(projectGPMSDS.getHost().getPort())
                .append("/")
                .append(projectGPMSDS.getName())
                .toString();
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    private DataSourceFactory() {
    }

}
