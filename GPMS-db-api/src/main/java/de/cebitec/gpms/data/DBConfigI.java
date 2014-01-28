package de.cebitec.gpms.data;

/**
 *
 * @author sjaenick
 */
public interface DBConfigI {

    public String getURI();

    public String getDatabaseHost();
    
    public int getDatabasePort();

    public String getDatabaseName();
}
