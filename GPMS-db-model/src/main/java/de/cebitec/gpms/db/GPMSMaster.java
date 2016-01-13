package de.cebitec.gpms.db;

import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.JPAMasterI;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class GPMSMaster implements JPAMasterI {

    private final static Logger logger = Logger.getLogger(GPMSMaster.class.getPackage().getName());
    private final MembershipI membership;
    private EntityManagerFactory emf = null;
    private final DataSource_DBI gpmsDataSource;
    private final DataSource dataSource;
    private UserI user;

    public GPMSMaster(MembershipI m, DataSource_DBI gpmsDataSource, DataSource ds) {
        this.membership = m;
        this.gpmsDataSource = gpmsDataSource;
        this.dataSource = ds;
    }

    public void setEntityManagerFactory(EntityManagerFactory ef) {
        emf = ef;
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    @Override
    public void close() {
        // valid e.g. for Hikari
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DataSource_DBI getGPMSDatasource() {
        return gpmsDataSource;
    }

    @Override
    public ProjectI getProject() {
        return membership.getProject();
    }

    @Override
    public RoleI getRole() {
        return membership.getRole();
    }

    @Override
    public void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void setUser(UserI user) {
        this.user = user;
    }

    @Override
    public UserI getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.membership);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPMSMaster other = (GPMSMaster) obj;
        if (!Objects.equals(this.membership, other.membership)) {
            return false;
        }
        return true;
    }

}
