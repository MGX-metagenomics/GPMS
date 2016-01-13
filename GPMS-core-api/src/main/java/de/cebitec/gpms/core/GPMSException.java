/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.core;

/**
 *
 * @author sjaenick
 */
public class GPMSException extends Exception {

    public GPMSException() {
    }

    public GPMSException(String message) {
        super(message);
    }

    public GPMSException(Throwable cause) {
        super(cause);
    }
    
}
