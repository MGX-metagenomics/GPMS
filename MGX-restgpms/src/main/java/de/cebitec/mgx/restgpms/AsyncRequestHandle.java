/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.restgpms;

import de.cebitec.gpms.rest.AsyncRequestHandleI;
import de.cebitec.gpms.rest.RESTException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author sj
 */
public class AsyncRequestHandle implements AsyncRequestHandleI {

    private final Future<Response> req;

    public AsyncRequestHandle(Future<Response> req) {
        this.req = req;
    }

    @Override
    public final boolean isDone() {
        return req.isDone();
    }

    @Override
    public boolean isSuccess() throws RESTException {
        
        while (!req.isDone()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
            }
        }
        
        try {
            Response res = req.get();
            if (Response.Status.fromStatusCode(res.getStatus()) != Response.Status.OK) {
                StringBuilder msg = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(res.readEntity(InputStream.class)))) {
                    String buf;
                    while ((buf = r.readLine()) != null) {
                        msg.append(buf);
                        msg.append(System.lineSeparator());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
                throw new RESTException(msg.toString().trim());
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new RESTException(ex);
        }
        return true;
    }

}
