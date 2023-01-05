/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.gpms.model;

import de.cebitec.gpms.core.UserI;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sjaenick
 */
public class UserTest {

    public UserTest() {
    }

    @Test
    public void testEquals() {
        System.out.println("equals");
        UserI u1 = new User("user", "pass1");
        UserI u2 = new User("user", "pass2");
        assertNotEquals(u1, u2);
    }

}
