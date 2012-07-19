package de.cebitec.gpms.core;


/**
 *
 * @author sjaenick
 */
public interface MembershipI<T extends ProjectI, U extends RoleI> {

    public T getProject();

    public U getRole();
}
