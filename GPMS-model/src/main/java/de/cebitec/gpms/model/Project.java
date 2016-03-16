package de.cebitec.gpms.model;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.ProjectI;
import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public class Project implements ProjectI {

    private final String name;
    private final ProjectClassI pclass;
    private final boolean isPublic;
    private final Collection<DataSourceI> dataSources;

    public Project(String name, ProjectClassI pclass, Collection<DataSourceI> dataSources, boolean isPublic) {
        if (name == null) {
            throw new IllegalArgumentException("No name supplied for project");
        }
        if (pclass == null) {
            throw new IllegalArgumentException("Null project class supplied for project " + name);
        }
        this.name = name;
        this.pclass = pclass;
        this.dataSources = dataSources;
        this.isPublic = isPublic;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProjectClassI getProjectClass() {
        return pclass;
    }

    @Override
    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public Collection<? extends DataSourceI> getDataSources() {
        return dataSources;
    }

}
