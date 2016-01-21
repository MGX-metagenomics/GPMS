package de.cebitec.gpms.db;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JPAMasterI;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class GPMSJPAMaster extends GPMSJDBCMaster implements JPAMasterI {

    private EntityManagerFactory emf = null;

    public GPMSJPAMaster(MembershipI m, DataSource_DBI gpmsDataSource, DataSource ds) {
        super(m, gpmsDataSource, ds);
    }

    public void setEntityManagerFactory(EntityManagerFactory ef) {
        emf = ef;
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
