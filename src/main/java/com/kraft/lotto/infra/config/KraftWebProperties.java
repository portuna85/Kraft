package com.kraft.lotto.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kraft.web")
public record KraftWebProperties(
        @DefaultValue("kraft.io.kr") String apexHost,
        @DefaultValue("https://www.kraft.io.kr") String canonicalOrigin
) {}
