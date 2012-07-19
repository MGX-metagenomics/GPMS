package de.cebitec.gpms.data;

import de.cebitec.gpms.core.RightI;
import de.cebitec.gpms.core.RoleI;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public interface DBRoleI extends RoleI {

    public String getDBPassword();

    public String getDBUser();

    public List<RightI> getRights();
}
