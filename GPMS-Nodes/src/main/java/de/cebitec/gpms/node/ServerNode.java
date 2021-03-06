package de.cebitec.gpms.node;

import de.cebitec.gpms.actions.DisconnectAction;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.rest.GPMSClientI;
import de.cebitec.gpms.nodefactory.ProjectNodeFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author sj
 */
public class ServerNode extends AbstractNode implements PropertyChangeListener {

    private final GPMSClientI gpmsclient;

    public ServerNode(GPMSClientI client) {
        super(Children.create(new ProjectNodeFactory(client), true), Lookups.singleton(client));
        this.gpmsclient = client;

        super.setDisplayName(gpmsclient.getServerName());
        setIconBaseWithExtension("de/cebitec/gpms/node/Server.png");

        UserI user = null;
        try {
            if (gpmsclient.loggedIn()) {
                user = gpmsclient.getUser();
            }
        } catch (GPMSException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (user != null) {
            super.setShortDescription(gpmsclient.getServerName() + " (Connected as " + user.getLogin() + ")");
        } else {
            super.setShortDescription(gpmsclient.getServerName() + " (Not connected)");
        }

        gpmsclient.addPropertyChangeListener(this);
        
        // needed here so all properties are registered; see property checking code
        // in Node#firePropertyChange()
        //
        getSheet();
    }

    @Override
    public Action[] getActions(boolean popup) {
        return new Action[]{new DisconnectAction()};
    }

    @Override
    protected Sheet createSheet() {
        Sheet ret = Sheet.createDefault();
        Sheet.Set set = ret.get(Sheet.PROPERTIES);

        Node.Property<Boolean> loginState = new PropertySupport.ReadOnly<Boolean>(GPMSClientI.PROP_LOGGEDIN, Boolean.class, "Login state", "Login state") {

            @Override
            public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
                return gpmsclient.loggedIn();
            }
        };
        set.put(loginState);

        return ret;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource().equals(gpmsclient) && GPMSClientI.PROP_LOGGEDIN.equals(evt.getPropertyName())) {
            if (gpmsclient.loggedIn()) {
                try {
                    setShortDescription(gpmsclient.getServerName() + " (Connected as " + gpmsclient.getUser().getLogin() + ")");
                } catch (GPMSException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else {
                setShortDescription(gpmsclient.getServerName() + " (Not connected)");
            }
        }
        // pass on all other events
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        gpmsclient.removePropertyChangeListener(this);
    }

}
