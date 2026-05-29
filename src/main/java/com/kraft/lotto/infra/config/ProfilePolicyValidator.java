package com.kraft.lotto.infra.config;

import java.util.List;
import org.springframework.core.env.ConfigurableEnvironment;

class ProfilePolicyValidator {

    private ProfilePolicyValidator() {
    }

    static void validate(ConfigurableEnvironment env, List<String> problems) {
        boolean inContainer = Boolean.parseBoolean(env.getProperty("KRAFT_IN_CONTAINER", "false"));
        String kraftEnv = env.getProperty("KRAFT_ENV", "").trim().toLowerCase();
        String[] activeProfiles = env.getActiveProfiles();
        String active = activeProfiles.length == 0 ? "<none>" : String.join(",", activeProfiles);
        boolean activeLocal = env.matchesProfiles("local");
        boolean activeProd = env.matchesProfiles("prod");

        if (inContainer) {
            if (!activeProd) {
                problems.add(RequiredConfigValidator.format(
                        "spring.profiles.active",
                        "active profile",
                        "KRAFT_IN_CONTAINER=true requires prod profile (current: " + active + ")"
                ));
            }
        } else if (!activeLocal) {
            problems.add(RequiredConfigValidator.format(
                    "spring.profiles.active",
                    "active profile",
                    "local runtime requires local profile (current: " + active + ")"
            ));
        }

        if (kraftEnv.isBlank()) {
            return;
        }

        if (!"local".equals(kraftEnv) && !"prod".equals(kraftEnv)) {
            problems.add(RequiredConfigValidator.format(
                    "KRAFT_ENV",
                    "environment discriminator (allowed: local|prod)",
                    "invalid value: " + kraftEnv
            ));
            return;
        }

        if ("local".equals(kraftEnv) && !activeLocal) {
            problems.add(RequiredConfigValidator.format(
                    "KRAFT_ENV",
                    "environment discriminator",
                    "KRAFT_ENV=local requires local profile (current: " + active + ")"
            ));
        } else if ("prod".equals(kraftEnv) && !activeProd) {
            problems.add(RequiredConfigValidator.format(
                    "KRAFT_ENV",
                    "environment discriminator",
                    "KRAFT_ENV=prod requires prod profile (current: " + active + ")"
            ));
        }
    }
}
