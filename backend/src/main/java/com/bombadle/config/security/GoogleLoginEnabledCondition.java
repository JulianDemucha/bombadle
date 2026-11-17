package com.bombadle.config.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Google sign-in is on only when both the client id and the client secret are set. */
public class GoogleLoginEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("bombadle.google.client-id"))
                && StringUtils.hasText(env.getProperty("bombadle.google.client-secret"));
    }
}
