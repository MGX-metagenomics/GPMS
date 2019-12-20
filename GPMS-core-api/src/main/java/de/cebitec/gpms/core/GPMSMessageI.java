/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.core;

import java.util.Date;

/**
 *
 * @author sj
 */
public interface GPMSMessageI extends Comparable<GPMSMessageI> {

    public Date getDate();

    public String getText();

}
