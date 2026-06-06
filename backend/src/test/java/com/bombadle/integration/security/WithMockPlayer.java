package com.bombadle.integration.security;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockPlayerSecurityContextFactory.class)
public @interface WithMockPlayer {
    String username() default "example@example.example";
    String role() default "ROLE_USER";
    boolean accountLocked() default false;
}