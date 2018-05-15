package de.cebitec.mgx.restgpms;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
public class GPMSClient implements GPMSClientI {

    private ClientConfig cc = null;
    private Client client;
    private final String gpmsBaseURI;
    private final String servername;
    private UserI user;
    private boolean loggedin = false;
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
        this.servername = servername;
        this.gpmsBaseURI = gpmsBaseURI;
        cc = new DefaultClientConfig();
        cc.getClasses().add(de.cebitec.mgx.protobuf.serializer.PBReader.class);
        cc.getClasses().add(de.cebitec.mgx.protobuf.serializer.PBWriter.class);
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, 10);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 10000); // in ms
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 30000);

        if (!requireSSL) {

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
        
        client = Client.create(cc);
    }

    @Override
    public Iterator<ProjectClassI> getProjectClasses() throws GPMSException {
        if (!loggedIn()) {
            throw new GPMSException("Not logged in.");
        }
        List<ProjectClassI> ret = new LinkedList<>();
        ClientResponse response = getResource().path("GPMS").path("GPMSBean").path("listProjectClasses").get(ClientResponse.class);
        if (Status.fromStatusCode(response.getStatus()) == Status.OK) {
            ProjectClassDTOList list = response.<ProjectClassDTOList>getEntity(ProjectClassDTOList.class);
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
            ClientResponse response = null;
            try {
                response = getResource().path("GPMS").path("GPMSBean").path("listMemberships").get(ClientResponse.class);
            } catch (ClientHandlerException ex) {
                if (ex.getCause() != null && ex.getCause() instanceof SSLHandshakeException) {
                    return getMemberships(); //retry
                } else if (ex.getCause() != null && ex.getCause() instanceof UnknownHostException) {
                    throw new GPMSException("Could not resolve server address. Check your internet connection.");
                }
                throw new GPMSException(ex.getCause().getMessage());
            }

            if (response != null && Status.fromStatusCode(response.getStatus()) == Status.OK) {
                MembershipDTOList list = response.<MembershipDTOList>getEntity(MembershipDTOList.class);
                for (MembershipDTO mdto : list.getMembershipList()) {

                    ProjectDTO projectDTO = mdto.getProject();
                    ProjectClassI pclass = new ProjectClass(projectDTO.getProjectClass().getName(), new HashSet<RoleI>());

                    for (RoleDTO rdto : mdto.getProject().getProjectClass().getRoles().getRoleList()) {
                        RoleI role = new Role(pclass, rdto.getName());
                        pclass.getRoles().add(role);
                    }

                    // create an artificial datasource based on the base URI for the app server
                    String projectBaseURI = projectDTO.hasBaseURI() && !projectDTO.getBaseURI().isEmpty()
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
                return null;
            }
        }
        return ret.iterator();
    }

    private WebResource getResource() {
        if (client == null) {
            return null;
        }
        return client.resource(gpmsBaseURI);
    }

    @Override
    public synchronized boolean login(String login, String password) throws GPMSException {
        if (loggedIn()) {
            throw new GPMSException("Already logged in as " + getUser().getLogin());

        }
        if (login == null || password == null) {
            return false;
        }
        client = Client.create(cc);
        client.removeAllFilters();
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        loggedin = false;
        user = null;

        ClientResponse response;
        try {
            response = getResource().path("GPMS").path("GPMSBean").path("login").get(ClientResponse.class);
        } catch (ClientHandlerException che) {
            if (che.getCause() != null && che.getCause() instanceof SSLHandshakeException) {
                SSLHandshakeException she = (SSLHandshakeException) che.getCause();
                System.err.println(she);
                return login(login, password);
            } else if (che.getCause() != null && che.getCause() instanceof UnknownHostException) {
                throw new GPMSException("Could not resolve server address. Check your internet connection.");
            }
            // most common cause here: server down
            throw new GPMSException(che.getCause().getMessage());
        }

        switch (Status.fromStatusCode(response.getStatus())) {
            case OK:
                GPMSString reply = response.<GPMSString>getEntity(GPMSString.class);
                if ("MGX".equals(reply.getValue())) {
                    loggedin = true;
                    user = new User(login, password, new ArrayList<MembershipI>());
                }
                break;
            case UNAUTHORIZED:
                throw new GPMSException("Wrong username/password");
            case GATEWAY_TIMEOUT:
                throw new GPMSException("Connection refused, server down?");
            default:
                throw new GPMSException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }

        pcs.firePropertyChange(PROP_LOGGEDIN, !loggedin, loggedin);

        return loggedin;
    }

    @Override
    public final long ping() {
        try {
            WebResource wr = getResource();
            if (wr == null) { // e.g. after logging out
                return -1;
            }
            return wr.path("GPMS").path("GPMSBean").path("ping").get(GPMSLong.class).getValue();
        } catch (UniformInterfaceException ufie) {
            System.err.println("GPMSClient MSG: " + ufie.getMessage());
        } catch (ClientHandlerException ex) {
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
            // set loggedin to false first, so calls to loggedIn() will return false
            loggedin = false;
            // notify all listeners of logout operation in progress
            // so they can execute shutdown hooks, if necessary
            pcs.firePropertyChange(PROP_LOGGEDIN, true, false);
            // after the property chance has been processed,
            // release resources
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
