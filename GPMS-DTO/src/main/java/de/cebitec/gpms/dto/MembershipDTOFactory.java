package de.cebitec.gpms.dto;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.dto.impl.MembershipDTO;
import de.cebitec.gpms.dto.impl.MembershipDTOList;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class MembershipDTOFactory extends DTOConversionBase<MembershipI, MembershipDTO, MembershipDTOList> {

    static {
        instance = new MembershipDTOFactory();
    }
    protected final static MembershipDTOFactory instance;

    private MembershipDTOFactory() {
    }

    public static MembershipDTOFactory getInstance() {
        return instance;
    }

    @Override
    public MembershipDTO toDTO(MembershipI m) {
        return MembershipDTO.newBuilder()
                .setProject(ProjectDTOFactory.getInstance().toDTO(m.getProject()))
                .setRole(RoleDTOFactory.getInstance().toDTO(m.getRole()))
                .build();
    }

    @Override
    public MembershipI toDB(MembershipDTO dto) {
        // not used
        return null;
    }

    @Override
    public MembershipDTOList toDTOList(Iterator<MembershipI> acit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
