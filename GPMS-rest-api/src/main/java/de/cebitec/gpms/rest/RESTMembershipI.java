/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.RoleI;

/**
 *
 * @author sj
 */
public interface RESTMembershipI<U extends RoleI> extends MembershipI<RESTProjectI, U> {
    
    @Override
    public RESTProjectI getProject();
}
