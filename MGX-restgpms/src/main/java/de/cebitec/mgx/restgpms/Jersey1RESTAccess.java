/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.RESTException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import de.cebitec.gpms.core.DataSource_ApplicationServerI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.rest.RESTAccessI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author sjaenick
 */
public class Jersey1RESTAccess implements RESTAccessI {

    public final static String PROTOBUF_TYPE = "application/x-protobuf";
    private final Client client;
    private final ClientConfig cc;
    private final URI resource;
    private final int numRetriesAllowed = 5;

    private final static boolean LOG_REQUESTS = false;

    public Jersey1RESTAccess(UserI user, DataSource_ApplicationServerI appServer, boolean verifySSL, Class... serializers) {
        cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, 10);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 10000); // in ms
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 90000); // in ms

        if (!verifySSL) {

            /*
             * taken from
             * http://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client
             * 
             * code below disables certificate validation; required for servers running
             * with self-signed or otherwise untrusted certificates
             */
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext ctx = null;
            try {
                ctx = SSLContext.getInstance("SSL");
                ctx.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                Logger.getLogger(RESTMaster.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
        }

        for (Class clazz : serializers) {
            if (!cc.getClasses().contains(clazz)) {
                cc.getClasses().add(clazz);
            }
        }

        cc.getClasses().add(de.cebitec.mgx.protobuf.serializer.PBReader.class);
        cc.getClasses().add(de.cebitec.mgx.protobuf.serializer.PBWriter.class);

        client = Client.create(cc);
        if (LOG_REQUESTS) {
            client.addFilter(new LoggingFilter(System.out));
        }
        client.addFilter(new HTTPBasicAuthFilter(user.getLogin(), user.getPassword()));
        this.resource = appServer.getURL();
    }

    /**
     *
     * @param <U>
     * @param path REST URI
     * @param obj object to send
     * @param c class of U
     * @return
     * @throws RESTException
     */
    @Override
    public final <U> U put(Object obj, Class<U> c, final String... path) throws RESTException {
        WebResource.Builder buildPath = buildPath(path);
        return put(obj, c, buildPath, numRetriesAllowed);
    }

    private <U> U put(Object obj, Class<U> c, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.put(ClientResponse.class, obj);
            catchException(res);
            return res.<U>getEntity(c);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                return put(obj, c, buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final void put(Object obj, final String... path) throws RESTException {
        WebResource.Builder buildPath = buildPath(path);
        put(obj, buildPath, numRetriesAllowed);
    }

    private void put(Object obj, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.put(ClientResponse.class, obj);
            catchException(res);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                put(obj, buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final void get(final String... path) throws RESTException {
        //System.err.println("GET uri: " +getWebResource().path(path).getURI().toASCIIString());
        WebResource.Builder buildPath = buildPath(path);
        get(buildPath, numRetriesAllowed);

    }

    private void get(WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.get(ClientResponse.class);
            catchException(res);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                get(buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final <U> U get(Class<U> c, final String... path) throws RESTException {
        WebResource.Builder buildPath = buildPath(path);
        return get(c, buildPath, numRetriesAllowed);
    }

    private <U> U get(Class<U> c, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.get(ClientResponse.class);
            catchException(res);
            return res.<U>getEntity(c);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                return get(c, buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final <U> U delete(Class<U> clazz, final String... path) throws RESTException {
        //System.err.println("DELETE uri: " +getWebResource().path(path).getURI().toASCIIString());
        WebResource.Builder buildPath = buildPath(path);
        return delete(clazz, buildPath, numRetriesAllowed);
    }

    private <U> U delete(Class<U> clazz, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.delete(ClientResponse.class);
            catchException(res);
            return res.<U>getEntity(clazz);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                return delete(clazz, buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final void delete(final String... path) throws RESTException {
        //System.err.println("DELETE uri: " +getWebResource().path(path).getURI().toASCIIString());
        WebResource.Builder buildPath = buildPath(path);
        delete(buildPath, numRetriesAllowed);
    }

    private void delete(WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.delete(ClientResponse.class);
            catchException(res);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                delete(buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public final void post(Object obj, final String... path) throws RESTException {
        WebResource.Builder buildPath = buildPath(path);
        post(obj, buildPath, numRetriesAllowed);
    }

    private void post(Object obj, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.post(ClientResponse.class, obj);
            catchException(res);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                post(obj, buildPath, numRetries - 1);  // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    @Override
    public <U> U post(Object obj, Class<U> targetClass, String... path) throws RESTException {
        WebResource.Builder buildPath = buildPath(path);
        return post(obj, targetClass, buildPath, numRetriesAllowed);
    }

    private <U> U post(Object obj, Class<U> targetClass, WebResource.Builder buildPath, int numRetries) throws RESTException {
        try {
            ClientResponse res = buildPath.post(ClientResponse.class, obj);
            catchException(res);
            return res.<U>getEntity(targetClass);
        } catch (ClientHandlerException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                return post(obj, targetClass, buildPath, numRetries - 1); //retry
            } else if (ex.getCause() != null && ex.getCause() instanceof Exception) {
                throw new RESTException(ex.getCause().getMessage());
            } else {
                throw ex; // rethrow
            }
        }
    }

    private WebResource.Builder buildPath(String... pathComponents) {
        WebResource wr = client.resource(resource);
        try {
            for (String s : pathComponents) {
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            }
            //System.err.println(wr.getURI().toASCIIString());
            return wr.type(PROTOBUF_TYPE).accept(PROTOBUF_TYPE);
        } catch (UnsupportedEncodingException ex) {
            throw new ClientHandlerException(ex);
        }
    }

    private void catchException(final ClientResponse res) throws RESTException {
        if (ClientResponse.Status.fromStatusCode(res.getStatus()) != ClientResponse.Status.OK) {
            StringBuilder msg = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getEntityInputStream()))) {
                String buf;
                while ((buf = r.readLine()) != null) {
                    msg.append(buf);
                    msg.append(System.lineSeparator());
                }
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
            throw new RESTException(msg.toString().trim());
        }
    }

//    @Override
//    public DataSourceTypeI getType() {
//        return new DataSourceTypeI() {
//
//            @Override
//            public String getName() {
//                return DataSourceTypeI.REST;
//            }
//
//        };
//    }
}
