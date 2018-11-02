package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.rest.GPMSClientFactory;
import de.cebitec.gpms.rest.GPMSClientI;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.fail;
import org.junit.Assume;

/**
 *
 * @author sj
 */
public class TestMaster {

    public static GPMSClientI get() {

        String serverURI = "https://mgx.cebitec.uni-bielefeld.de/MGX-maven-web/webresources/";

        String config = System.getProperty("user.home") + "/.m2/gpms.junit";
        File f = new File(config);
        if (f.exists() && f.canRead()) {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(f));
                serverURI = p.getProperty("testserver");
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }

        Assume.assumeNotNull(serverURI);
        try {
            URL myURL = new URL(serverURI);
            URLConnection myURLConnection = myURL.openConnection();
            myURLConnection.connect();
        } catch (MalformedURLException e) {
            fail("Invalid URL");
        } catch (IOException e) {
            Assume.assumeFalse("Could not connect to "+serverURI, true);
        }

        GPMSClientI gpms = null;
        try {
            gpms = GPMSClientFactory.createClient("MyServer", serverURI, false);
        } catch (GPMSException ex) {
            Logger.getLogger(TestMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        return gpms;
    }
}
