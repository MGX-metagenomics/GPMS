package de.cebitec.gpms.dto;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.dto.impl.RoleDTO;
import de.cebitec.gpms.dto.impl.RoleDTOList;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class RoleDTOFactory extends DTOConversionBase<RoleI, RoleDTO, RoleDTOList> {

    static {
        instance = new RoleDTOFactory();
    }
    protected final static RoleDTOFactory instance;

    private RoleDTOFactory() {
    }

    public static RoleDTOFactory getInstance() {
        return instance;
    }

    @Override
    public RoleDTO toDTO(RoleI r) {
        return RoleDTO.newBuilder()
                .setName(r.getName())
                .build();
    }

    @Override
    public RoleI toDB(RoleDTO dto) {
        // not used
        return null;
    }

    @Override
    public RoleDTOList toDTOList(Iterator<RoleI> acit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
