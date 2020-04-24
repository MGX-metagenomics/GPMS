/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

/**
 *
 * @author sjaenick
 */
public class RESTDisconnectedException extends RESTException {

    public RESTDisconnectedException() {
        super("GPMS REST access has been closed.");
    }

    public RESTDisconnectedException(Throwable cause) {
        super(cause);
    }
    
}
