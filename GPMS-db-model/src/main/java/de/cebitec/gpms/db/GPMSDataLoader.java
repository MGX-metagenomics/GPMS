/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.db;

import de.cebitec.gpms.util.GPMSDataLoaderI;
import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.gpms.util.GPMSDataSourceSelector;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public abstract class GPMSDataLoader implements GPMSDataLoaderI {

    private EntityManagerFactory emf = null;
    private final ThreadLocal<MasterI> currentMaster = new ThreadLocal<>();

    @Override
    public <T extends MasterI> T getCurrentMaster() {
        return (T) currentMaster.get();
    }

    @Override
    public <T extends MasterI> void setCurrentMaster(T master) {
        currentMaster.set(master);
    }

    @Override
    public <T extends MasterI> T createMaster(MembershipI mbr, Class<T> masterClass) throws GPMSException {

        T ret = null;

        if (JDBCMasterI.class.isAssignableFrom(masterClass)) {

            // find an appropriate GPMS datasource to work with
            DataSourceI selectedGPMSdataSource = GPMSDataSourceSelector.selectDataSource(mbr, masterClass);
            if (selectedGPMSdataSource == null) {
                throw new GPMSException("No appropriate datasource could be found for project " + mbr.getProject().getName());
            }

            if (!(selectedGPMSdataSource instanceof DataSource_DBI)) {
                throw new GPMSException("Cannot create SQL-based master without database-backed datasource for " + mbr.getProject().getName());
            }

            DataSource_DBI dsDB = (DataSource_DBI) selectedGPMSdataSource;
            final String[] dbAuth = getDatabaseCredentials(mbr.getRole());

            if (dbAuth == null || dbAuth.length != 2) {
                throw new GPMSException("Server does not support " + mbr.getProject().getProjectClass().getName() + " projects.");
            }

            // create SQL datasource
            DataSource ds = DataSourceFactory.createDataSource(mbr, dsDB, dbAuth[0], dbAuth[1]);

            if (JPAMasterI.class.isAssignableFrom(masterClass)) {
                GPMSJPAMaster jpaMaster = new GPMSJPAMaster(mbr, dsDB, ds);

                // current master needs to be set _before_ creating the EMF
                currentMaster.set(jpaMaster);

                if (emf == null) {
                    emf = EMFNameResolver.createEMF(mbr, ProxyDataSourceI.JNDI_NAME); //, "MGX-PU");
                }
                jpaMaster.setEntityManagerFactory(emf);

                ret = (T) jpaMaster;

            } else {
                // JDBC master
                JDBCMasterI jdbcMaster = new GPMSJDBCMaster(mbr, dsDB, ds);
                currentMaster.set(jdbcMaster);
                ret = (T) jdbcMaster;
            }

        } else {
            // simple master
            MasterI simpleMaster = new GPMSSimpleMaster(mbr);
            currentMaster.set(simpleMaster);
            ret = (T) simpleMaster;
        }

        return ret;
    }

}
