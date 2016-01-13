package de.cebitec.gpms.data;

import javax.persistence.EntityManagerFactory;

/**
 *
 * @author sjaenick
 */
public interface JPAMasterI extends JDBCMasterI {

    public EntityManagerFactory getEntityManagerFactory();

}
