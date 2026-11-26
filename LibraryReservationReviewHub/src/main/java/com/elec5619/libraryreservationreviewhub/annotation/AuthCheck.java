package com.elec5619.libraryreservationreviewhub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that needs a logged-in caller. Enforced by
 * {@link com.elec5619.libraryreservationreviewhub.config.AuthInterceptor}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * Role the caller must have, e.g. {@code UserConstant.ADMIN_ROLE}. Empty means any logged-in user.
     **/
    String mustRole() default "";
}
