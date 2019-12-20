/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.GPMSMessageI;
import java.util.Date;

/**
 *
 * @author sj
 */
public class GPMSMessage implements GPMSMessageI {
    
    private final Date date;
    private final String message;

    public GPMSMessage(Date date, String message) {
        this.date = date;
        this.message = message;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getText() {
        return message;
    }

    @Override
    public int compareTo(GPMSMessageI t) {
        return getDate().compareTo(t.getDate());
    }
    
}
