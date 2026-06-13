package com.kraft.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.security")
public record SecurityProperties(
        String trustedProxyCidr,
        int rateLimitPerMinute,
        int rateLimitMaxKeys
) {
    public SecurityProperties {
        if (trustedProxyCidr == null || trustedProxyCidr.isBlank()) {
            trustedProxyCidr = "172.28.0.0/16";
        }
        if (rateLimitPerMinute <= 0) {
            rateLimitPerMinute = 120;
        }
        if (rateLimitMaxKeys <= 0) {
            rateLimitMaxKeys = 10_000;
        }
    }
}
