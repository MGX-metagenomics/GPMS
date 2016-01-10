package de.cebitec.gpms.dto;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.dto.impl.ProjectClassDTO;
import de.cebitec.gpms.dto.impl.ProjectClassDTOList;
import de.cebitec.gpms.dto.impl.RoleDTOList;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class ProjectClassDTOFactory extends DTOConversionBase<ProjectClassI, ProjectClassDTO, ProjectClassDTOList> {

    static {
        instance = new ProjectClassDTOFactory();
    }
    protected final static ProjectClassDTOFactory instance;

    private ProjectClassDTOFactory() {
    }

    public static ProjectClassDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ProjectClassDTO toDTO(ProjectClassI pc) {
        RoleDTOList.Builder roles = RoleDTOList.newBuilder();
        for (RoleI r : pc.getRoles()) {
            roles.addRole(RoleDTOFactory.getInstance().toDTO(r));
        }
        return ProjectClassDTO.newBuilder()
                .setName(pc.getName())
                .setRoles(roles.build())
                .build();
    }

    @Override
    public ProjectClassI toDB(ProjectClassDTO dto) {
        // not used
        return null;
    }

    @Override
    public ProjectClassDTOList toDTOList(Iterator<ProjectClassI> iter) {
        ProjectClassDTOList.Builder ret = ProjectClassDTOList.newBuilder();
        while (iter.hasNext()) {
            ret.addProjectClass(toDTO(iter.next()));
}
        return ret.build();
    }
}
