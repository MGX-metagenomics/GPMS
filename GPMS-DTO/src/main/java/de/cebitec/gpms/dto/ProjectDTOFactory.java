package de.cebitec.gpms.dto;

import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.dto.impl.ProjectDTO;
import de.cebitec.gpms.dto.impl.ProjectDTOList;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class ProjectDTOFactory extends DTOConversionBase<ProjectI, ProjectDTO, ProjectDTOList> {

    static {
        instance = new ProjectDTOFactory();
    }
    protected final static ProjectDTOFactory instance;

    private ProjectDTOFactory() {
    }

    public static ProjectDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ProjectDTO toDTO(ProjectI p) {
        return ProjectDTO.newBuilder()
                .setName(p.getName())
                .setProjectClass(ProjectClassDTOFactory.getInstance().toDTO(p.getProjectClass()))
                .build();
    }

    @Override
    public ProjectI toDB(ProjectDTO dto) {
        // not used
        return null;
    }

    @Override
    public ProjectDTOList toDTOList(Iterator<ProjectI> acit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
