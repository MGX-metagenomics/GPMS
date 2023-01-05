package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.GPMSClientI;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 *
 * @author sj
 */
public class TestMaster {

    public static GPMSClientI get() {

        String serverURI = "https://mgx-test.computational.bio.uni-giessen.de/MGX-maven-web/webresources/";

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

        assumeTrue(serverURI != null);
        try {
            URL myURL = new URL(serverURI);
            URLConnection myURLConnection = myURL.openConnection();
            myURLConnection.connect();
        } catch (MalformedURLException e) {
            fail("Invalid URL");
        } catch (IOException e) {
            assumeFalse(true, "Could not connect to " + serverURI);
        }

        GPMSClientI gpms = null;
        gpms = new GPMSClient("MyServer", serverURI, false);
        return gpms;
    }
}
