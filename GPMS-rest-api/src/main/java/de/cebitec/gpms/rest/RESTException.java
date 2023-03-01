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
public class RESTException extends Exception {

    @Serial
    private static final long serialVersionUID = 6401253773779951803L;

    public RESTException(String message) {
        super(message);
    }

    public RESTException(Throwable cause) {
        super(cause);
    }

}
