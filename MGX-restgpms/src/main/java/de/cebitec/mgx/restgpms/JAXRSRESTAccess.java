/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.RESTException;
import de.cebitec.gpms.core.DataSource_ApplicationServerI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.rest.RESTAccessI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
//import org.glassfish.jersey.client.ClientConfig;
//import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author sjaenick
 */
public class JAXRSRESTAccess implements RESTAccessI {

    public final static String PROTOBUF_TYPE = "application/x-protobuf";

    //private final ClientConfig cc;
    private final WebTarget wt;
    private final int numRetriesAllowed = 5;

    private final static boolean LOG_REQUESTS = false;

    public JAXRSRESTAccess(UserI user, URI appServerURI, boolean verifySSL, Class... serializers) {

        //cc = new ClientConfig();
        SSLContext ctx = null;
        HostnameVerifier verifier = null;

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

            try {
                ctx = SSLContext.getInstance("SSL");
                ctx.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                Logger.getLogger(RESTMaster.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Create all-trusting host name verifier
            verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(verifier);

            //cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
        }

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);

        Client client;

        ResteasyClientBuilder cb = ((ResteasyClientBuilder) ClientBuilder
                .newBuilder())
                .httpEngine(engine);

        for (Class clazz : serializers) {
            cb.register(clazz);
        }

//        cb.register(de.cebitec.mgx.protobuf.serializer.PBReader.class);
//        cb.register(de.cebitec.mgx.protobuf.serializer.PBWriter.class);
        //cb.register(new HTTPAuthenticator(user.getLogin(), user.getPassword()));
        if (ctx != null && verifier != null) {
            client = cb
                    //.withConfig(cc)
                    .sslContext(ctx)
                    .hostnameVerifier(verifier)
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .build();

        } else {
            client = cb
                    //.withConfig(cc)
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .build();
        }

        wt = client.target(appServerURI);

        wt.register(new BasicAuthentication(user.getLogin(), user.getPassword()));

        wt.register(de.cebitec.mgx.protobuf.serializer.PBReader.class);
        wt.register(de.cebitec.mgx.protobuf.serializer.PBWriter.class);
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
        Invocation.Builder buildPath = buildPath(path);
        return put(obj, c, buildPath, numRetriesAllowed);
    }

    private <U> U put(Object obj, Class<U> c, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.put(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
            return res.readEntity(c);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        put(obj, buildPath, numRetriesAllowed);
    }

    private void put(Object obj, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.put(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        get(buildPath, numRetriesAllowed);

    }

    private void get(Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.get(Response.class)) {
            catchException(res);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        return get(c, buildPath, numRetriesAllowed);
    }

    private <U> U get(Class<U> c, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.get(Response.class)) {
            catchException(res);
            return res.readEntity(c);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        return delete(clazz, buildPath, numRetriesAllowed);
    }

    private <U> U delete(Class<U> clazz, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.delete(Response.class)) {
            catchException(res);
            return res.readEntity(clazz);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        delete(buildPath, numRetriesAllowed);
    }

    private void delete(Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.delete(Response.class)) {
            catchException(res);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        post(obj, buildPath, numRetriesAllowed);
    }

    private void post(Object obj, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.post(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
        } catch (ProcessingException ex) {
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
        Invocation.Builder buildPath = buildPath(path);
        return post(obj, targetClass, buildPath, numRetriesAllowed);
    }

    private <U> U post(Object obj, Class<U> targetClass, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try (Response res = buildPath.post(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
            return res.readEntity(targetClass);
        } catch (ProcessingException ex) {
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

    private Invocation.Builder buildPath(String... pathComponents) {
        WebTarget wr = wt; //client.target(resource);
        try {
            for (String s : pathComponents) {
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            }
            //System.err.println(wr.getURI().toASCIIString());

            return wr.request(PROTOBUF_TYPE).accept(PROTOBUF_TYPE);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void catchException(final Response res) throws RESTException {
        if (Response.Status.fromStatusCode(res.getStatus()) != Response.Status.OK) {
            StringBuilder msg = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.readEntity(InputStream.class)))) {
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

}
