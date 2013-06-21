package de.cebitec.gpms.data;

import de.cebitec.gpms.core.MasterI;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface DBMasterI extends MasterI {

    public EntityManagerFactory getEntityManagerFactory();

    public DataSource getDataSource();

    @Override
    public DBProjectI getProject();

    public void setLogin(String login);

    public String getLogin();
    
    public void log(String message);
}
