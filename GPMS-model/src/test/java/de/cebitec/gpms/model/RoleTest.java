/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sjaenick
 */
public class RoleTest {

    @Test
    public void testEquals() {
        System.out.println("equals");
        ProjectClassI pClass1 = new ProjectClass("PClass1");
        ProjectClassI pClass2 = new ProjectClass("PClass2");
        RoleI r1 = new Role(pClass1, "Rolename");
        RoleI r2 = new Role(pClass1, "Rolename");
        RoleI r3 = new Role(pClass2, "Rolename");
        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
    }

}
