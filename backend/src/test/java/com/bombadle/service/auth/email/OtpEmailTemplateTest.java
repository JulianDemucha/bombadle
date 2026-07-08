package com.bombadle.service.auth.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpEmailTemplateTest {

    @Test
    void build_embedsCodeCopyAndLogoWithoutScripts() {
        String html = OtpEmailTemplate.build(
                "Reset hasła", "Treść wiadomości.", "123456", 15, "Uwaga bezpieczeństwa.");

        assertThat(html)
                .contains("123456")
                .contains("Reset hasła")
                .contains("Treść wiadomości.")
                .contains("15 minut")
                .contains("Uwaga bezpieczeństwa.")
                .contains("cid:" + OtpEmailTemplate.LOGO_CID)
                .contains("monospace")
                .doesNotContain("<script")
                .doesNotContain("onclick");
    }
}
