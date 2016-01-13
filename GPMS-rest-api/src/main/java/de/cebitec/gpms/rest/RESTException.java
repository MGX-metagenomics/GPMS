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
public class RESTException extends Exception {

    public RESTException(String message) {
        super(message);
    }

    public RESTException(Throwable cause) {
        super(cause);
    }
    
}
