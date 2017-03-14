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
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author sj
 */
public class DatasourceProvider implements DataSourceProviderI {

    private final Cache<CacheKey, GPMSManagedDataSourceI> datasourceCache;
    private final Timer timer;

    public DatasourceProvider() {
        datasourceCache = CacheBuilder.<CacheKey, GPMSManagedDataSourceI>newBuilder()
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
                cleanUp();
            }
        }, 10_000, 60_000 * 2);
    }

    void cleanUp() {
        datasourceCache.cleanUp();
    }

    @Override
    public GPMSManagedDataSourceI getDataSource(RoleI role, DataSource_DBI gpms_DS) {
        return datasourceCache.getIfPresent(new CacheKey(gpms_DS, role));
    }

    private void closeDataSource(GPMSManagedDataSourceI gpmsManagedDataSource) {
        gpmsManagedDataSource.close(datasourceCache);
    }

    @Override
    public GPMSManagedDataSourceI registerDataSource(RoleI role, DataSource_DBI gpms_DS, DataSource sqlDatasource) {
        GPMSManagedDataSourceI gpmsManagedDataSource = datasourceCache.getIfPresent(new CacheKey(gpms_DS, role));
        if (gpmsManagedDataSource == null) {
            gpmsManagedDataSource = new GPMSManagedDataSource(sqlDatasource, gpms_DS, role);
            //
            // a "fake" subscription which is withdrawn by the removal listener
            //
            gpmsManagedDataSource.subscribe(datasourceCache);
            datasourceCache.put(new CacheKey(gpms_DS, role), gpmsManagedDataSource);
        }
        return gpmsManagedDataSource;
    }

    @Override
    public final void dispose() {
        datasourceCache.invalidateAll();
        datasourceCache.cleanUp();
        timer.cancel();
    }
}
