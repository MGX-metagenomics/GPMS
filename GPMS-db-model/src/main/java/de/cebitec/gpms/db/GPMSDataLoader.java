/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.db;

import de.cebitec.gpms.db.sql.DataSourceFactory;
import de.cebitec.gpms.util.GPMSDataLoaderI;
import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.db.sql.DatasourceProvider;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.gpms.util.GPMSDataSourceSelector;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import de.cebitec.gpms.util.DataSourceProviderI;

/**
 *
 * @author sjaenick
 */
public abstract class GPMSDataLoader implements GPMSDataLoaderI {

    private EntityManagerFactory emf = null;
    private final ThreadLocal<MasterI> currentMaster = new ThreadLocal<>();
    private final DataSourceProviderI dsProvider = new DatasourceProvider();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MasterI> T getCurrentMaster() {
        return (T) currentMaster.get();
    }

    @Override
    public void setCurrentMaster(MasterI master) {
        currentMaster.set(master);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MasterI> T createMaster(MembershipI mbr, Class<T> masterClass) throws GPMSException {

        if (!JDBCMasterI.class.isAssignableFrom(masterClass)) {
            //
            // only a simple master instance without database connections
            // is requested
            //
            MasterI simpleMaster = new GPMSSimpleMaster(mbr);
            currentMaster.set(simpleMaster);
            return (T) simpleMaster;
        }

        /*
        *  all other master instances require a db connection
        * 
         */
        //
        // find an appropriate GPMS datasource to work with
        //
        DataSourceI selectedGPMSdataSource = GPMSDataSourceSelector.selectDataSource(mbr, masterClass);
        if (selectedGPMSdataSource == null) {
            throw new GPMSException("No appropriate datasource could be found for project " + mbr.getProject().getName());
        }

        if (!(selectedGPMSdataSource instanceof DataSource_DBI)) {
            throw new GPMSException("Cannot create SQL-based master without database-backed datasource for " + mbr.getProject().getName());
        }

        DataSource_DBI dsDB = (DataSource_DBI) selectedGPMSdataSource;

        //
        // find the backing SQL datasource (or create a new one)
        //
        GPMSManagedDataSourceI gpmsManagedDataSource = dsProvider.getDataSource(mbr.getRole(), dsDB);
        if (gpmsManagedDataSource == null) {
            final String[] dbAuth = getDatabaseCredentials(mbr.getRole());

            if (dbAuth == null || dbAuth.length != 2) {
                throw new GPMSException("Server does not support " + mbr.getProject().getProjectClass().getName() + " projects.");
            }

            // create new SQL datasource
            DataSource sqlDatasource = DataSourceFactory.createDataSource(mbr, dsDB, dbAuth[0], dbAuth[1]);
            
            // add to cache
            gpmsManagedDataSource = dsProvider.registerDataSource(mbr.getRole(), dsDB, sqlDatasource);
        }
        // add to cache
        //GPMSManagedDataSourceI gpmsManagedDataSource = dsProvider.registerDataSource(mbr.getRole(), dsDB, sqlDatasource);

        //
        // create plain JDBC or JPA master depending on requested master class
        //
        if (JPAMasterI.class.isAssignableFrom(masterClass)) {
            GPMSJPAMaster jpaMaster = new GPMSJPAMaster(mbr, dsDB, gpmsManagedDataSource);
            // current master needs to be set _before_ creating the EMF
            currentMaster.set(jpaMaster);
            if (emf == null) {
                emf = EMFNameResolver.createEMF(mbr, ProxyDataSourceI.JNDI_NAME);
            }
            jpaMaster.setEntityManagerFactory(emf);
            return (T) jpaMaster;

        } else {
            // JDBC master
            JDBCMasterI jdbcMaster = new GPMSJDBCMaster(mbr, dsDB, gpmsManagedDataSource);
            currentMaster.set(jdbcMaster);
            return (T) jdbcMaster;
        }
    }

    @Override
    public void dispose() {
        dsProvider.dispose();
    }

}
