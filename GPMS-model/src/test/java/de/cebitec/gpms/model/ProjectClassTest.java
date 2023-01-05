/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sjaenick
 */
public class ProjectClassTest {

    @Test
    public void testEquals() {
        System.out.println("equals");
        ProjectClass pc1 = new ProjectClass("FOO");
        ProjectClass pc2 = new ProjectClass("FOO");
        ProjectClass pc3 = new ProjectClass("BAR");
        assertEquals(pc1, pc2);
        assertNotEquals(pc1, pc3);
    }

}
