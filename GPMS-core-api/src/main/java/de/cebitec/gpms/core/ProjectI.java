package de.cebitec.gpms.core;

import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public interface ProjectI {

    public String getName();

    public ProjectClassI getProjectClass();

    public boolean isPublic();

    public Collection<? extends DataSourceI> getDataSources();

}
