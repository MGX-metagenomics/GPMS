/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.core;

import java.io.Serial;

/**
 *
 * @author sjaenick
 */
public class GPMSException extends Exception {

    @Serial
    private static final long serialVersionUID = 6401253773779951803L;

    public GPMSException() {
    }

    public GPMSException(String message) {
        super(message);
    }

    public GPMSException(Throwable cause) {
        super(cause);
    }

    public GPMSException(String message, Throwable cause) {
        super(message, cause);
    }

}
