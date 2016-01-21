package de.cebitec.gpms.db;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class GPMSJDBCMaster extends GPMSSimpleMaster implements JDBCMasterI {

    private final DataSource_DBI gpmsDataSource;
    private final DataSource dataSource;
    //
    private static final Logger LOG = Logger.getLogger(GPMSJDBCMaster.class.getName());
    

    public GPMSJDBCMaster(MembershipI m, DataSource_DBI gpmsDataSource, DataSource ds) {
        super(m);
        this.gpmsDataSource = gpmsDataSource;
        this.dataSource = ds;
    }

    @Override
    public void close() {
        super.close();
        // valid e.g. for Hikari
        if (dataSource instanceof AutoCloseable) {
            try {
                LOG.log(Level.INFO, "Closing SQL connection pool for {0}/{1}", new Object[]{gpmsDataSource.getName(), getRole().getName()});
                ((AutoCloseable) dataSource).close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public final DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public final DataSource_DBI getGPMSDatasource() {
        return gpmsDataSource;
    }

}
