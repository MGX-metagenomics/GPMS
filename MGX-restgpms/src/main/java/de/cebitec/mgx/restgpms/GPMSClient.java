package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSourceTypeI;
import de.cebitec.gpms.core.DataSource_ApplicationServerI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.dto.impl.GPMSLong;
import de.cebitec.gpms.dto.impl.GPMSString;
import de.cebitec.gpms.dto.impl.MembershipDTO;
import de.cebitec.gpms.dto.impl.MembershipDTOList;
import de.cebitec.gpms.dto.impl.ProjectClassDTO;
import de.cebitec.gpms.dto.impl.ProjectClassDTOList;
import de.cebitec.gpms.dto.impl.ProjectDTO;
import de.cebitec.gpms.dto.impl.RoleDTO;
import de.cebitec.gpms.model.GPMSDataSourceAppServer;
import de.cebitec.gpms.model.Membership;
import de.cebitec.gpms.model.Project;
import de.cebitec.gpms.model.ProjectClass;
import de.cebitec.gpms.model.Role;
import de.cebitec.gpms.model.User;
import de.cebitec.gpms.rest.GPMSClientI;
import de.cebitec.gpms.rest.RESTMasterI;
import static de.cebitec.mgx.restgpms.JAXRSRESTAccess.PROTOBUF_TYPE;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
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
public class GPMSClient implements GPMSClientI {

    //private final ClientConfig cc;
    private ApacheHttpClient43Engine engine;
    private Client client;
    private WebTarget wt;
    private final String gpmsBaseURI;
    private final String servername;
    private UserI user;
    private boolean loggedin = false;
    private boolean validateSSL = true;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final static DataSourceTypeI REST_DSTYPE = new DataSourceTypeI() {
        @Override
        public String getName() {
            return "artificial REST datasource type";
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.getName());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DataSourceTypeI other = (DataSourceTypeI) obj;
            return Objects.equals(this.getName(), other.getName());
        }

    };

    public GPMSClient(String servername, String gpmsBaseURI) {
        this(servername, gpmsBaseURI, true);
    }

    public GPMSClient(String servername, String gpmsBaseURI, boolean requireSSL) {
        if (gpmsBaseURI == null) {
            throw new IllegalArgumentException("No base URI supplied.");
        }
        if (!gpmsBaseURI.endsWith("/")) {
            gpmsBaseURI += "/";
        }
        if (requireSSL && !gpmsBaseURI.startsWith("https://")) {
            throw new IllegalArgumentException("Secure connection required.");
        }
        this.validateSSL = requireSSL;
        this.servername = servername;
        this.gpmsBaseURI = gpmsBaseURI;

//        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
//        CloseableHttpClient httpClient;
//        if (!validateSSL) {
//            SSLContext sslContext = null;
//            try {
//                sslContext = SSLContextBuilder
//                        .create()
//                        .loadTrustMaterial(new TrustSelfSignedStrategy())
//                        .build();
//            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
//                Logger.getLogger(JAXRSRESTAccess.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
//            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
//
//            httpClient = HttpClients
//                    .custom()
//                    .setConnectionManager(cm)
//                    .setSSLSocketFactory(connectionFactory)
//                    .build();
//        } else {
//            httpClient = HttpClients.custom().setConnectionManager(cm).build();
//        }
//        cm.setMaxTotal(200); // Increase max total connection to 200
//        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
//        engine = new ApacheHttpClient43Engine(httpClient);
//
//        ResteasyClientBuilder cb = ((ResteasyClientBuilder) ClientBuilder
//                .newBuilder())
//                .httpEngine(engine);
//
//        if (!validateSSL) {
//            client = cb
//                    .disableTrustManager()
//                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
//                    .readTimeout(100000, TimeUnit.MILLISECONDS)
//                    .build();
//
//        } else {
//            client = cb
//                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
//                    .readTimeout(100000, TimeUnit.MILLISECONDS)
//                    .build();
//        }
//
//        wt = client.target(gpmsBaseURI);
//
//        wt.register(de.cebitec.mgx.protobuf.serializer.PBReader.class);
//        wt.register(de.cebitec.mgx.protobuf.serializer.PBWriter.class);
    }

    @Override
    public Iterator<ProjectClassI> getProjectClasses() throws GPMSException {
        if (!loggedIn()) {
            throw new GPMSException("Not logged in.");
        }
        List<ProjectClassI> ret = new LinkedList<>();
        Response response = getResource("GPMS", "GPMSBean", "listProjectClasses").get(Response.class);
        if (Status.fromStatusCode(response.getStatus()) == Status.OK) {
            ProjectClassDTOList list = response.readEntity(ProjectClassDTOList.class);
            for (ProjectClassDTO dto : list.getProjectClassList()) {

                ProjectClassI pClass = new ProjectClass(dto.getName(), new HashSet<RoleI>());

                for (RoleDTO rdto : dto.getRoles().getRoleList()) {
                    RoleI role = new Role(pClass, rdto.getName());
                    pClass.getRoles().add(role);
                }

                ret.add(pClass);
            }
        }
        return ret.iterator();
    }

    @Override
    public RESTMasterI createMaster(final MembershipI m) throws GPMSException {
        if (!loggedIn()) {
            throw new GPMSException("Not logged in.");
        }
        if (m == null) {
            throw new GPMSException("REST MembershipI is null");
        }
        return new RESTMaster(this, m, user);
    }

    @Override
    public Iterator<MembershipI> getMemberships() throws GPMSException {
        List<MembershipI> ret = new ArrayList<>();
        if (loggedIn()) {
            Response response = null;
            try {
                response = getResource("GPMS", "GPMSBean", "listMemberships").get(Response.class);
            } catch (ProcessingException ex) {
                if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                    return getMemberships(); //retry
                } else if (ex.getCause() != null && ex.getCause() instanceof UnknownHostException) {
                    throw new GPMSException("Could not resolve server address. Check your internet connection.");
                }
                throw new GPMSException(ex.getCause().getMessage());
            }

            if (response != null && Status.fromStatusCode(response.getStatus()) == Status.OK) {
                MembershipDTOList list = response.readEntity(MembershipDTOList.class);
                response.close();
                for (MembershipDTO mdto : list.getMembershipList()) {

                    ProjectDTO projectDTO = mdto.getProject();
                    ProjectClassI pclass = new ProjectClass(projectDTO.getProjectClass().getName(), new HashSet<RoleI>());

                    for (RoleDTO rdto : mdto.getProject().getProjectClass().getRoles().getRoleList()) {
                        RoleI role = new Role(pclass, rdto.getName());
                        pclass.getRoles().add(role);
                    }

                    // create an artificial datasource based on the base URI for the app server
                    String projectBaseURI = !projectDTO.getBaseURI().isEmpty()
                            ? projectDTO.getBaseURI()
                            : gpmsBaseURI + projectDTO.getName();
                    URI dsURI;
                    try {
                        dsURI = new URI(projectBaseURI);
                    } catch (URISyntaxException ex) {
                        throw new GPMSException(ex);
                    }

                    // reconstruct artificial GPMS appserver datasource from project DTO data
                    DataSource_ApplicationServerI restDS = new GPMSDataSourceAppServer("artificial REST datasource", dsURI, REST_DSTYPE);

                    List<DataSourceI> dsList = new ArrayList<>(1);
                    dsList.add(restDS);
                    ProjectI proj = new Project(projectDTO.getName(), pclass, dsList, false);

                    RoleI role = new Role(pclass, mdto.getRole().getName());

                    ret.add(new Membership(proj, role));
                }
            } else {
                if (response != null) {
                    response.close();
                }
                return null;
            }
        }
        return ret.iterator();
    }

    private Invocation.Builder getResource(String... pathComponents) {
        if (client == null) {
            return null;
        }
        WebTarget wr = wt;
        for (String s : pathComponents) {
            try {
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(GPMSClient.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return wr.request(PROTOBUF_TYPE).accept(PROTOBUF_TYPE);
    }

    @Override
    public synchronized boolean login(String login, char[] password) throws GPMSException {
        return login(login, new String(password));
    }

    @Override
    public synchronized boolean login(String login, String password) throws GPMSException {
        return login(login, password, 5);
    }

    private boolean login(String login, String password, int maxAttempts) throws GPMSException {
        if (loggedIn()) {
            throw new GPMSException("Already logged in as " + getUser().getLogin());

        }
        if (login == null || password == null) {
            return false;
        }

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient;
        if (!validateSSL) {
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
                    .setSSLSocketFactory(connectionFactory)
                    .build();
        } else {
            httpClient = HttpClients.custom().setConnectionManager(cm).build();
        }
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

        // discard previous client and engine, if present
        if (client != null) {
            client.close();
            client = null;
        }
        if (engine != null) {
            engine.close();
            engine = null;
        }
        engine = new ApacheHttpClient43Engine(httpClient);

        ResteasyClientBuilder cb = ((ResteasyClientBuilder) ClientBuilder
                .newBuilder())
                .httpEngine(engine);

        if (!validateSSL) {
            client = cb
                    .disableTrustManager()
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .build();

        } else {
            client = cb
                    .connectTimeout(10000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .build();
        }

        wt = client.target(gpmsBaseURI);
        wt.register(new BasicAuthentication(login, password));

        wt.register(de.cebitec.mgx.protobuf.serializer.PBReader.class);
        wt.register(de.cebitec.mgx.protobuf.serializer.PBWriter.class);

        loggedin = false;
        user = null;

        Response response;
        try {
            response = getResource("GPMS", "GPMSBean", "login").get(Response.class);
        } catch (ProcessingException che) {

            client.close();
            engine.close();
            client = null;
            engine = null;

            if (che.getCause() != null && che.getCause() instanceof SSLHandshakeException) {
                SSLHandshakeException she = (SSLHandshakeException) che.getCause();
                Throwable t = she.getCause();
                while (t.getCause() != null) {
                    t = t.getCause();
                }
                if (t instanceof CertificateExpiredException) {
                    throw new GPMSException("Server SSL certificate expired: " + t.getMessage());
                }
                if (maxAttempts > 0) {
                    return login(login, password, maxAttempts - 1);
                }
            } else if (che.getCause() != null && che.getCause() instanceof UnknownHostException) {
                throw new GPMSException("Could not resolve server address. Check your internet connection.");
            }
            // most common cause here: server down
            throw new GPMSException(che.getCause().getMessage());
        }

        switch (Status.fromStatusCode(response.getStatus())) {
            case OK:
                GPMSString reply = response.readEntity(GPMSString.class);
                if ("MGX".equals(reply.getValue())) {
                    loggedin = true;
                    user = new User(login, password, new ArrayList<MembershipI>());
                }
                break;
            case UNAUTHORIZED:
                client.close();
                engine.close();
                client = null;
                engine = null;
                throw new GPMSException("Wrong username/password");
            case GATEWAY_TIMEOUT:
                client.close();
                engine.close();
                client = null;
                engine = null;
                throw new GPMSException("Connection refused, server down?");
            default:
                client.close();
                engine.close();
                client = null;
                engine = null;
                throw new GPMSException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }

        if (!loggedin) {
            client.close();
            engine.close();
            client = null;
            engine = null;
        }

        pcs.firePropertyChange(PROP_LOGGEDIN, !loggedin, loggedin);

        return loggedin;
    }

    @Override
    public final long ping() {
        if (!loggedIn()) {
            return -1;
        }
        
        try {
            Invocation.Builder wr = getResource("GPMS", "GPMSBean", "ping");
            if (wr == null) { // e.g. after logging out
                return -1;
            }
            return wr.get(GPMSLong.class).getValue();
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                return ping(); //retry
            } else if (ex.getCause() != null && ex.getCause() instanceof UnknownHostException) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public synchronized void logout() {
        if (loggedIn()) {
            // set loggedin to false first, so subsequent calls to loggedIn() will return false
            loggedin = false;
            // notify all listeners of logout operation in progress
            // so they can execute shutdown hooks, if necessary
            pcs.firePropertyChange(PROP_LOGGEDIN, true, false);
            // after the property chance has been processed,
            // release resources
            client.close();
            engine.close();
            engine = null;
            client = null;
            user = null;
        }
    }

    @Override
    public String getBaseURI() {
        return gpmsBaseURI;
    }

    @Override
    public final String getServerName() {
        return servername;
    }

    @Override
    public final UserI getUser() throws GPMSException {
        if (!loggedIn()) {
            throw new GPMSException("Not logged in.");
        }
        return user;
    }

    @Override
    public final boolean loggedIn() {
        return loggedin;
    }

    @Override
    public boolean validateSSL() {
        return validateSSL;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.servername);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPMSClient other = (GPMSClient) obj;
        if (!Objects.equals(this.gpmsBaseURI, other.gpmsBaseURI)) {
            return false;
        }
        return Objects.equals(this.servername, other.servername);
    }

}
