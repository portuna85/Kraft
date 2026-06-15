package com.kraft.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.external-lotto")
public record ExternalLottoProperties(
        String urlTemplate
) {

    public boolean enabled() {
        return urlTemplate != null && !urlTemplate.isBlank();
    }
}
