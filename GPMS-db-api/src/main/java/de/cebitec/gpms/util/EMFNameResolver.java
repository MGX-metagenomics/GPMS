package de.cebitec.gpms.util;

import de.cebitec.gpms.core.MembershipI;
import java.util.*;
import javax.persistence.EntityManagerFactory;

/**
 * A EMFNameResolver can be implemented as a service. It is used to load a persistenceunit
 * that is not reflected by the project_class, for example when there are different versions
 * of a project-database. Each may need a separate PU and services that implement this
 * interface can resolve the right PU name.
 *
 * @author ljelonek
 */
public abstract class EMFNameResolver {

    /**
     * Determines whether the EMFNameResolver should be used to resolve a PU for
     * a specific ProjectMembership.
     * @param pm
     * @return
     */
    public abstract boolean handles(MembershipI pm);

    public abstract String resolve(MembershipI pm);

    public abstract EntityManagerFactory create(String pu, Properties config);

    /**
     * Checks all EMFNameResolvers whether they can resolve a projectclass to a
     * persistence unit name. If this succeeds the PU name is returned. Otherwise
     * the project class name is returned.
     *
     * @param pm
     * @return
     */
    public static String resolvePUName(MembershipI pm) {
        for (Iterator<EMFNameResolver> it = resolvers.keySet().iterator(); it.hasNext();) {
            EMFNameResolver eMFNameResolver = it.next();
            if (eMFNameResolver.handles(pm)) {
                return eMFNameResolver.resolve(pm);
            }
        }
        return pm.getProject().getProjectClass().getName();
    }

    public static EntityManagerFactory createEMF(MembershipI pm, String jndi, String pu) {
        Properties props = new Properties();
        props.setProperty("javax.persistence.jtaDataSource", jndi);

        for (Iterator<EMFNameResolver> it = resolvers.keySet().iterator(); it.hasNext();) {
            EMFNameResolver eMFNameResolver = it.next();
            if (eMFNameResolver.handles(pm)) {
                // it may be, that the registered EMFResolver has a different classloader
                // that can't be accessed from the thread classloader. So we simply change
                // the active classloader to the right one for PU creation.
                ClassLoader tmp = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(eMFNameResolver.getClass().getClassLoader());
                EntityManagerFactory emf = eMFNameResolver.create(pu, props);
                Thread.currentThread().setContextClassLoader(tmp);
                resolvers.get(eMFNameResolver).add(emf);
                return emf;
            }
        }
        throw new UnsupportedOperationException("No EMFResolver for Persistence Unit " + pu);
    }
    private static HashMap<EMFNameResolver, List<EntityManagerFactory>> resolvers = new HashMap<>();

    public static void registerResolver(EMFNameResolver resolver) {
        if (!resolvers.containsKey(resolver)) {
            resolvers.put(resolver, new LinkedList<EntityManagerFactory>());
        }
    }

    public static void unregisterResolver(EMFNameResolver resolver) {
        for (EntityManagerFactory emf : resolvers.get(resolver)) {
            emf.close();
        }
        resolvers.remove(resolver);
    }
}
