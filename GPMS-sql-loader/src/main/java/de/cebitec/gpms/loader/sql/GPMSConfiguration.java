package de.cebitec.gpms.loader.sql;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class GPMSConfiguration {

    private String configDir;

    public String getGPMSConfigDirectory() {
        return configDir;
    }

    @PostConstruct
    public void startup() {

        String cfgFile = new StringBuilder(System.getProperty("user.dir"))
                .append(File.separator)
                .append("gpms.properties")
                .toString();

        File f = new File(cfgFile);
        if (!f.exists()) {
            throw new RuntimeException("GPMS configuration failed: " + cfgFile + " missing");
        }

        Properties config = new Properties();
        FileInputStream in = null;

        try {
            in = new FileInputStream(cfgFile);
            config.load(in);
            in.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        configDir = config.getProperty("gpms_configdir");
    }
}
