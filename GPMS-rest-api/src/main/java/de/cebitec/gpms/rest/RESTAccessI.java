/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.rest;

import java.io.Closeable;

/**
 *
 * @author sjaenick
 */
public interface RESTAccessI extends Closeable {

    public void get(final String... path) throws RESTException;

    public <U> U get(Class<U> targetClass, final String... path) throws RESTException;

    public void put(Object obj, final String... path) throws RESTException;

    public <U> U put(Object obj, Class<U> targetClass, final String... path) throws RESTException;

    public void post(Object obj, final String... path) throws RESTException;

    public <U> U post(Object obj, Class<U> targetClass, final String... path) throws RESTException;

//    public AsyncRequestHandleI postAsync(Object obj, final String... path);

    public void delete(final String... path) throws RESTException;

    public <U> U delete(Class<U> targetClass, final String... path) throws RESTException;

    public void addFilter(Object filter);

}
