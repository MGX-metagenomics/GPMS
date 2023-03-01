/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

import java.io.Serial;

/**
 *
 * @author sjaenick
 */
public class RESTDisconnectedException extends RESTException {

    @Serial
    private static final long serialVersionUID = 6401253773779951803L;

    public RESTDisconnectedException() {
        super("GPMS REST access has been closed.");
    }

    public RESTDisconnectedException(Throwable cause) {
        super(cause);
    }

}
