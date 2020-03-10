/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

/**
 *
 * @author sj
 */
public interface AsyncRequestHandleI {

    boolean isDone();

    boolean isSuccess() throws RESTException;
    
}
