package com.cutm.smo.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require specific user roles.
 * Used in conjunction with AuthorizationAspect for method-level authorization.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * The required role(s) to access the method.
     */
    String[] value() default {};
}
