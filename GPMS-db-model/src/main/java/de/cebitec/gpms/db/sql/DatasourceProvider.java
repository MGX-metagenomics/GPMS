/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.db.sql;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import de.cebitec.gpms.util.DataSourceProviderI;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author sj
 */
public class DatasourceProvider implements DataSourceProviderI {
    
    private static class CacheKey {
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
            if (!Objects.equals(this.role, other.role)) {
                return false;
            }
            return true;
        }
    }

    private final Cache<CacheKey, GPMSManagedDataSourceI> datasourceCache;
    private final Timer timer;

    public DatasourceProvider() {
        datasourceCache = CacheBuilder.newBuilder()
                .expireAfterAccess(6, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<CacheKey, GPMSManagedDataSourceI>() {

                    @Override
                    public void onRemoval(RemovalNotification<CacheKey, GPMSManagedDataSourceI> notification) {
                        closeDataSource(notification.getValue());
                    }
                })
                .build();

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                datasourceCache.cleanUp();
            }
        }, 10_000, 60_000 * 2);
    }

    @Override
    public DataSource getDataSource(RoleI role, DataSource_DBI gpms_DS) {
        return datasourceCache.getIfPresent(gpms_DS);
    }

    private void closeDataSource(GPMSManagedDataSourceI gpmsManagedDataSource) {
        gpmsManagedDataSource.close();
    }

    @Override
    public GPMSManagedDataSourceI registerDataSource(RoleI role, DataSource_DBI gpms_DS, DataSource sqlDatasource) {
        GPMSManagedDataSourceI gpmsManagedDataSource = datasourceCache.getIfPresent(gpms_DS);
        if (gpmsManagedDataSource == null) {
            gpmsManagedDataSource = new GPMSManagedDataSource(sqlDatasource, gpms_DS, role);
            //
            // a "fake" subscription which is withdrawn by the removal listener
            //
            gpmsManagedDataSource.subscribe();
            datasourceCache.put(new CacheKey(gpms_DS, role), gpmsManagedDataSource);
        }
        return gpmsManagedDataSource;
    }

    @Override
    public final void dispose() {
        timer.cancel();
        datasourceCache.invalidateAll();
    }
}
