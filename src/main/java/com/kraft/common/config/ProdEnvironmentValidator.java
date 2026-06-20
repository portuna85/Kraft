package com.kraft.common.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProdEnvironmentValidator {

    @Value("${kraft.public-base-url:}")
    private String publicBaseUrl;

    private final Environment environment;
    private final ExternalLottoProperties externalLottoProperties;
    private final RevalidateProperties revalidateProperties;
    private final OpsProperties opsProperties;

    public ProdEnvironmentValidator(Environment environment,
                                    ExternalLottoProperties externalLottoProperties,
                                    RevalidateProperties revalidateProperties,
                                    OpsProperties opsProperties) {
        this.environment = environment;
        this.externalLottoProperties = externalLottoProperties;
        this.revalidateProperties = revalidateProperties;
        this.opsProperties = opsProperties;
    }

    @PostConstruct
    void validate() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        List<String> missing = missingRequiredVariables();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "prod profile requires the following environment variables to be set: " + missing);
        }
    }

    List<String> missingRequiredVariables() {
        List<String> missing = new ArrayList<>();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            missing.add("KRAFT_PUBLIC_BASE_URL");
        }
        if (externalLottoProperties.urlTemplate() == null || externalLottoProperties.urlTemplate().isBlank()) {
            missing.add("KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE");
        }
        if (revalidateProperties.secret() == null || revalidateProperties.secret().isBlank()) {
            missing.add("KRAFT_REVALIDATE_SECRET");
        }
        if (opsProperties.token() == null || opsProperties.token().isBlank()) {
            missing.add("KRAFT_OPS_TOKEN");
        }
        return missing;
    }
}
