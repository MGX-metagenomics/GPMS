package de.cebitec.gpms.db;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class GPMSJDBCMaster extends GPMSSimpleMaster implements JDBCMasterI {

    private final DataSource_DBI gpmsDataSource;
     private final GPMSManagedDataSourceI dataSource;
    //
    private static final Logger LOG = Logger.getLogger(GPMSJDBCMaster.class.getName());

    public GPMSJDBCMaster(MembershipI m, DataSource_DBI gpmsDataSource, GPMSManagedDataSourceI ds) {
        super(m);
        this.gpmsDataSource = gpmsDataSource;
        this.dataSource = ds;
        this.dataSource.subscribe();
    }

    @Override
    public void close() {
        super.close();
        LOG.log(Level.INFO, "Closing GPMSJDBCMaster for {0}/{1}", new Object[]{getProject().getName(), getRole().getName()});
        dataSource.close();
    }

    @Override
    public final GPMSManagedDataSourceI getDataSource() {
        return dataSource;
    }

    @Override
    public final DataSource_DBI getGPMSDatasource() {
        return gpmsDataSource;
    }
    
    
}
