/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.util;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public abstract class GPMSDataSourceSelector {

    public abstract <T extends MasterI> DataSourceI selectFromDataSources(MembershipI mbr, Class<T> masterClass) throws GPMSException;

    private final static ConcurrentMap<String, GPMSDataSourceSelector> selectors = new ConcurrentHashMap<>();
    protected static final Logger LOG = Logger.getLogger(GPMSDataSourceSelector.class.getName());

    public final static void registerSelector(String pClass, GPMSDataSourceSelector sel) {
        LOG.log(Level.INFO, "Registered DataSource selector for project class {0}", pClass);
        selectors.put(pClass, sel);
    }

    public final static void unregisterSelector(String pClass) {
        selectors.remove(pClass);
    }

    public final static <T extends MasterI> DataSourceI selectDataSource(MembershipI mbr, Class<T> masterClass) throws GPMSException {
        ProjectClassI projectClass = mbr.getProject().getProjectClass();
        if (selectors.containsKey(projectClass.getName())) {
            GPMSDataSourceSelector s = selectors.get(projectClass.getName());
            return s.selectFromDataSources(mbr, masterClass);
        }
        throw new GPMSException("No registered handler for project class "+ projectClass.getName());
    }
}
