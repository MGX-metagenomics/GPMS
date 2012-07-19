package de.cebitec.gpms.security;

import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.enterprise.util.Nonbinding;

/**
 * Declares the GPMS rights needed to run a method or all methods of a class.
 * Method annotations override class annotations.
 *
 * @author ljelonek
 */
@Inherited
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Secure {

    @Nonbinding
    String[] rightsNeeded() default {};
}