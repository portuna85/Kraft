package com.kraft.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kraft.saved")
public record SavedProperties(
        @Min(1) @Max(500) int maxPerClient
) {
}
