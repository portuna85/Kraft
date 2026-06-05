package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.admin")
@Validated
public record KraftAdminProperties(
        boolean enabled,
        List<String> allowedEmails,
        @NotBlank String adminDomain
) {
    public KraftAdminProperties {
        allowedEmails = allowedEmails == null ? List.of() : List.copyOf(allowedEmails);
    }
}
