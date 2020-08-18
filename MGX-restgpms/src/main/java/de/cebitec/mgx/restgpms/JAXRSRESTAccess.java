/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.RESTException;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.rest.RESTAccessI;
import de.cebitec.gpms.rest.RESTDisconnectedException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;

/**
 *
 * @author sjaenick
 */
public class JAXRSRESTAccess implements RESTAccessI {

    public final static String PROTOBUF_TYPE = "application/x-protobuf";

    private final Client client;
    private final ApacheHttpClient43Engine engine;
    private final WebTarget wt;
    private final int numRetriesAllowed = 5;
    private volatile boolean closed = false;

    public JAXRSRESTAccess(UserI user, URI appServerURI, boolean verifySSL, Class... serializers) {

        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultSocketConfig(socketConfig);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(15 * 1000)
                .setConnectionRequestTimeout(300 * 1000)
                .setSocketTimeout(300 * 1000)
                .build();

        CloseableHttpClient httpClient;
        if (!verifySSL) {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
                Logger.getLogger(JAXRSRESTAccess.class.getName()).log(Level.SEVERE, null, ex);
            }

            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

            httpClient = HttpClients
                    .custom()
                    .setConnectionManager(cm)
                    .setSSLSocketFactory(connectionFactory)
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .build();
        } else {
            httpClient = HttpClients
                    .custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .build();
        }
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(30); // Increase default max connection per route to 20
        engine = new ApacheHttpClient43Engine(httpClient);

        ResteasyClientBuilder cb = ((ResteasyClientBuilder) ClientBuilder
                .newBuilder())
                .httpEngine(engine);

        for (Class clazz : serializers) {
            cb.register(clazz);
        }

        cb.connectTimeout(10000, TimeUnit.MILLISECONDS);
        cb.readTimeout(100000, TimeUnit.MILLISECONDS);
        cb.connectionCheckoutTimeout(2, TimeUnit.MINUTES);

        if (!verifySSL) {
            cb.disableTrustManager();
        }

        client = cb.build();
        wt = client.target(appServerURI);

        if (user != null) {
            wt.register(new BasicAuthentication(user.getLogin(), user.getPassword()));
        }

        wt.register(de.cebitec.mgx.protobuf.serializer.PBReader.class);
        wt.register(de.cebitec.mgx.protobuf.serializer.PBWriter.class);
    }

    @Override
    public void addFilter(Object crf) {
        wt.register(crf);
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
        try ( Response res = buildPath.put(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
            return res.readEntity(c);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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
        try ( Response res = buildPath.put(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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
        try ( Response res = buildPath.get(Response.class)) {
            catchException(res);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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
        try ( Response res = buildPath.get(Response.class)) {
            catchException(res);
            return res.readEntity(c);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                if (numRetries == 0) {
                    throw ex;
                }
                return get(c, buildPath, numRetries - 1); // retry
            } else if (ex instanceof ProcessingException) {
                System.err.println("GOT YOU, retry "+ numRetries);
                if (numRetries == 0) {
                    throw new RESTException(ex.getMessage());
                }
                return get(c, buildPath, numRetries - 1); // retry
            } else if (ex.getCause() != null && ex.getCause() instanceof PrivilegedActionException) {
                throw new RESTException(ex.getCause().getMessage());
            } else if (ex.getCause() != null && ex.getCause() instanceof SocketTimeoutException) {
                throw new RESTException(ex.getCause().getMessage());
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
        try ( Response res = buildPath.delete(Response.class)) {
            catchException(res);
            return res.readEntity(clazz);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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
        try ( Response res = buildPath.delete(Response.class)) {
            catchException(res);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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
        try ( Response res = buildPath.post(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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

//    @Override
//    public final AsyncRequestHandleI postAsync(Object obj, final String... path) {
//        Invocation.Builder buildPath = buildPath(path);
//        Future<Response> res = buildPath.async().post(Entity.entity(obj, PROTOBUF_TYPE));
//        return new AsyncRequestHandle(res);
//    }
    @Override
    public <U> U post(Object obj, Class<U> targetClass, String... path) throws RESTException {
        Invocation.Builder buildPath = buildPath(path);
        return post(obj, targetClass, buildPath, numRetriesAllowed);
    }

    private <U> U post(Object obj, Class<U> targetClass, Invocation.Builder buildPath, int numRetries) throws RESTException {
        try ( Response res = buildPath.post(Entity.entity(obj, PROTOBUF_TYPE))) {
            catchException(res);
            return res.readEntity(targetClass);
        } catch (IllegalStateException | ProcessingException ex) {
            if (closed) {
                throw new RESTDisconnectedException();
            }
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

    private Invocation.Builder buildPath(String... pathComponents) throws RESTException {
        WebTarget wr = wt;

        if (closed) {
            throw new RESTDisconnectedException();
        }

        try {
            for (String s : pathComponents) {
                if (s == null || s.isEmpty()) {
                    throw new RESTException("Empty path component encountered.");
                }
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            }
            //System.err.println(wr.getURI().toASCIIString());

            return wr.request(PROTOBUF_TYPE).accept(PROTOBUF_TYPE);
        } catch (IllegalStateException ise) {
            // recheck if close happened
            if (closed || ise.getMessage().contains("Client is closed.")) {
                throw new RESTDisconnectedException();
            }
            throw ise; //rethrow
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void catchException(final Response res) throws RESTException {
        if (Response.Status.fromStatusCode(res.getStatus()) != Response.Status.OK) {
            StringBuilder msg = new StringBuilder();
            try ( BufferedReader r = new BufferedReader(new InputStreamReader(res.readEntity(InputStream.class)))) {
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

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            client.close();
            engine.close();
        }
    }
}
