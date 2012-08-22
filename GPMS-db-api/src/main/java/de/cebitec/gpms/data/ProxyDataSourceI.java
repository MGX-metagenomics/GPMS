package de.cebitec.gpms.data;

import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface ProxyDataSourceI extends DataSource {
    
    public static final String JNDI_NAME = "GPMSDataSourceRouter";

}
