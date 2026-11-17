package com.bombadle.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleLoginEnabledConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Probe.class);

    @Test
    void disabled_whenNoCredentialsAreSet() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean("googleMarker"));
    }

    @Test
    void disabled_whenCredentialsAreBlank() {
        runner.withPropertyValues("bombadle.google.client-id=", "bombadle.google.client-secret=")
                .run(ctx -> assertThat(ctx).doesNotHaveBean("googleMarker"));
    }

    @Test
    void disabled_whenOnlyClientIdIsSet() {
        runner.withPropertyValues("bombadle.google.client-id=id")
                .run(ctx -> assertThat(ctx).doesNotHaveBean("googleMarker"));
    }

    @Test
    void disabled_whenOnlyClientSecretIsSet() {
        runner.withPropertyValues("bombadle.google.client-secret=secret")
                .run(ctx -> assertThat(ctx).doesNotHaveBean("googleMarker"));
    }

    @Test
    void enabled_whenBothCredentialsAreSet() {
        runner.withPropertyValues("bombadle.google.client-id=id", "bombadle.google.client-secret=secret")
                .run(ctx -> assertThat(ctx).hasBean("googleMarker"));
    }

    @Configuration(proxyBeanMethods = false)
    static class Probe {
        @Bean
        @Conditional(GoogleLoginEnabledCondition.class)
        String googleMarker() {
            return "on";
        }
    }
}
