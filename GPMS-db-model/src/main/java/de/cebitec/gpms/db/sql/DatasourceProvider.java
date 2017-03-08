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

    private final Cache<DataSource_DBI, GPMSManagedDataSourceI> datasourceCache;
    private final Timer timer;

    public DatasourceProvider() {
        datasourceCache = CacheBuilder.newBuilder()
                .expireAfterAccess(6, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<DataSource_DBI, GPMSManagedDataSourceI>() {

                    @Override
                    public void onRemoval(RemovalNotification<DataSource_DBI, GPMSManagedDataSourceI> notification) {
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
    public DataSource getDataSource(DataSource_DBI gpms_DS) {
        return datasourceCache.getIfPresent(gpms_DS);
    }

    private void closeDataSource(GPMSManagedDataSourceI gpmsManagedDataSource) {
        gpmsManagedDataSource.close();
    }

    @Override
    public GPMSManagedDataSourceI registerDataSource(DataSource_DBI gpms_DS, DataSource sqlDatasource) {
        GPMSManagedDataSourceI gpmsManagedDataSource = datasourceCache.getIfPresent(gpms_DS);
        if (gpmsManagedDataSource == null) {
            gpmsManagedDataSource = new GPMSManagedDataSource(sqlDatasource, gpms_DS);
            //
            // a "fake" subscription which is withdrawn by the removal listener
            //
            gpmsManagedDataSource.subscribe();
            datasourceCache.put(gpms_DS, gpmsManagedDataSource);
        }
        return gpmsManagedDataSource;
    }

    @Override
    public final void dispose() {
        timer.cancel();
        datasourceCache.invalidateAll();
    }
}
