package com.kraft.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.revalidate")
public record RevalidateProperties(
        String secret,
        String webUrl
) {
    public boolean enabled() {
        return secret != null && !secret.isBlank()
               && webUrl != null && !webUrl.isBlank();
    }
}
