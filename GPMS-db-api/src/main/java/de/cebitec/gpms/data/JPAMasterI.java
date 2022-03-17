package de.cebitec.gpms.data;

import jakarta.persistence.EntityManagerFactory;


/**
 *
 * @author sjaenick
 */
public interface JPAMasterI extends JDBCMasterI {

    public EntityManagerFactory getEntityManagerFactory();

}
