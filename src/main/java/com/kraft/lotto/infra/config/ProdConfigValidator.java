package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.winningnumber.application.LottoApiClientConfig;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.ConfigurableEnvironment;

class ProdConfigValidator {

    private static final int OPS_TOKEN_MIN_LENGTH = 32;
    private static final Set<String> WEAK_TOKEN_PATTERNS = Set.of(
            "changeme", "change-me", "password", "secret", "test", "admin", "token"
    );

    private ProdConfigValidator() {
    }

    static void validate(ConfigurableEnvironment env, List<String> problems) {
        if (!env.matchesProfiles("prod")) {
            return;
        }
        requireNonBlank(env, problems, "kraft.api.url", "External API URL (env: KRAFT_API_URL)");
        requireStrongOpsToken(env, problems);
        requireIntInRange(env, problems, "kraft.recommend.max-attempts",
                "Recommend max attempts (env: KRAFT_RECOMMEND_MAX_ATTEMPTS)", 1, 1_000_000);
        requireIntInRange(env, problems, "kraft.recommend.initial-pick-max-attempts",
                "Recommend initial pick max attempts (env: KRAFT_RECOMMEND_INITIAL_PICK_MAX_ATTEMPTS)", 1, 1_000_000);
        requireIntInRange(env, problems, "kraft.recommend.fixup-max-attempts",
                "Recommend fixup max attempts (env: KRAFT_RECOMMEND_FIXUP_MAX_ATTEMPTS)", 1, 1_000_000);
        requireIntInRange(env, problems, "kraft.recommend.rules.birthday-threshold",
                "Recommend birthday threshold (env: KRAFT_RECOMMEND_RULE_BIRTHDAY_THRESHOLD)", 1, 44);
        requireIntInRange(env, problems, "kraft.recommend.rules.long-run-threshold",
                "Recommend long-run threshold (env: KRAFT_RECOMMEND_RULE_LONG_RUN_THRESHOLD)", 2, 6);
        requireIntInRange(env, problems, "kraft.recommend.rules.decade-threshold",
                "Recommend decade threshold (env: KRAFT_RECOMMEND_RULE_DECADE_THRESHOLD)", 3, 6);

        String apiClient = RequiredConfigValidator.safeGet(env, "kraft.api.client");
        Set<String> allowedProdClients = LottoApiClientConfig.prodAllowedClientTokens();
        if (apiClient == null || apiClient.isBlank()
                || !allowedProdClients.contains(apiClient.trim().toLowerCase())) {
            problems.add(RequiredConfigValidator.format(
                    "kraft.api.client",
                    "Lotto API client mode (env: KRAFT_API_CLIENT)",
                    "prod profile requires one of: " + allowedProdClients
            ));
        }
    }

    private static void requireStrongOpsToken(ConfigurableEnvironment env, List<String> problems) {
        String key = "kraft.security.ops.required-token";
        String desc = "Ops API token (env: KRAFT_SECURITY_OPS_REQUIRED_TOKEN)";
        String value = RequiredConfigValidator.safeGet(env, key);
        if (value == null || value.isBlank()) {
            problems.add(RequiredConfigValidator.format(key, desc, "required in prod profile but blank"));
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() < OPS_TOKEN_MIN_LENGTH) {
            problems.add(RequiredConfigValidator.format(key, desc,
                    "must be at least " + OPS_TOKEN_MIN_LENGTH + " characters in prod profile (actual: " + trimmed.length() + ")"));
            return;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String weak : WEAK_TOKEN_PATTERNS) {
            if (lower.contains(weak)) {
                problems.add(RequiredConfigValidator.format(key, desc,
                        "token looks like a placeholder — set a strong random token in prod profile"));
                return;
            }
        }
    }

    private static void requireNonBlank(ConfigurableEnvironment env,
                                        List<String> problems,
                                        String key,
                                        String desc) {
        String value = RequiredConfigValidator.safeGet(env, key);
        if (value == null || value.isBlank()) {
            problems.add(RequiredConfigValidator.format(key, desc, "required in prod profile but blank"));
        }
    }

    private static void requireIntInRange(ConfigurableEnvironment env,
                                          List<String> problems,
                                          String key,
                                          String desc,
                                          int min,
                                          int max) {
        String value = RequiredConfigValidator.safeGet(env, key);
        if (value == null || value.isBlank()) {
            problems.add(RequiredConfigValidator.format(key, desc, "required in prod profile but blank"));
            return;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                problems.add(RequiredConfigValidator.format(key, desc,
                        "must be between " + min + " and " + max + " (actual: " + parsed + ")"));
            }
        } catch (NumberFormatException ex) {
            problems.add(RequiredConfigValidator.format(key, desc, "must be an integer (actual: " + value + ")"));
        }
    }
}
