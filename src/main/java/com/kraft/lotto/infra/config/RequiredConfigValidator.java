package com.kraft.lotto.infra.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates required runtime configuration before the app starts.
 * Orchestrates focused sub-validators for each concern.
 */
public class RequiredConfigValidator implements EnvironmentPostProcessor, Ordered {

    public static final int ORDER = DotenvEnvironmentPostProcessor.ORDER + 100;
    static final String NL = System.lineSeparator();

    private static final Map<String, String> REQUIRED = new LinkedHashMap<>();

    static {
        REQUIRED.put("spring.datasource.url", "DB JDBC URL (env: KRAFT_DB_URL or KRAFT_DB_HOST/KRAFT_DB_PORT/KRAFT_DB_NAME)");
        REQUIRED.put("spring.datasource.username", "DB username (env: KRAFT_DB_USER)");
        REQUIRED.put("spring.datasource.password", "DB password (env: KRAFT_DB_PASSWORD)");
    }

    private static final List<String> REQUIRED_DEPLOY_ENV_VARS = List.of(
            "KRAFT_DB_NAME",
            "KRAFT_DB_USER",
            "KRAFT_DB_PASSWORD",
            "KRAFT_DB_ROOT_PASSWORD",
            "KRAFT_SECURITY_OPS_REQUIRED_TOKEN"
    );

    private static final Pattern ENV_HINT_PATTERN = Pattern.compile("\\(env:\\s*([A-Z0-9_]+)");

    public static List<String> requiredDeployEnvVars() {
        return REQUIRED_DEPLOY_ENV_VARS;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (isRunningUnderTest(env)) {
            return;
        }

        List<String> problems = new ArrayList<>();
        validateRequiredEntries(env, problems);
        JdbcConnectivityValidator.validate(env, problems);
        ProdConfigValidator.validate(env, problems);
        ProfilePolicyValidator.validate(env, problems);

        if (!problems.isEmpty()) {
            String msg = """
                    %n\
                    ============================================================%n\
                    KraftLotto startup validation failed.%n\
                    Please review environment variables and active profile.%n\
                    Active profiles: %s%n\
                    ============================================================%n\
                    %s%n\
                    %n\
                    Quick checks:%n\
                      - Verify required values are available as environment variables or .env entries.%n\
                      - Confirm each missing key has a matching env var value.%n\
                      - If running with Docker, ensure expected profile and env vars are set.%n\
                    """.formatted(activeProfilesText(env), String.join(System.lineSeparator(), problems));
            throw new StartupValidationException(msg);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static void validateRequiredEntries(ConfigurableEnvironment env, List<String> problems) {
        for (Map.Entry<String, String> entry : REQUIRED.entrySet()) {
            String key = entry.getKey();
            String raw;
            try {
                raw = env.getProperty(key);
            } catch (RuntimeException placeholderFail) {
                problems.add(format(key, entry.getValue(), "placeholder resolution failed: " + placeholderFail.getMessage()));
                continue;
            }
            if (raw == null || raw.isBlank()) {
                problems.add(format(key, entry.getValue(), "value is blank"));
                continue;
            }
            if (raw.contains("${")) {
                problems.add(format(key, entry.getValue(), "unresolved placeholder: " + raw));
            }
        }
    }

    static String format(String key, String desc, String reason) {
        StringBuilder line = new StringBuilder("  - [").append(key).append("] ").append(desc).append(" => ").append(reason);
        String envHint = extractEnvHint(desc);
        if (envHint != null) {
            line.append(NL).append("      - action: set ").append(envHint);
        }
        return line.toString();
    }

    static String safeGet(ConfigurableEnvironment env, String key) {
        try {
            return env.getProperty(key);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isRunningUnderTest(ConfigurableEnvironment env) {
        return System.getProperty("org.gradle.test.worker") != null
                || "true".equalsIgnoreCase(System.getProperty("kraft.skip.required-config-validator"))
                || Boolean.parseBoolean(env.getProperty("kraft.skip.required-config-validator", "false"));
    }

    private static String activeProfilesText(ConfigurableEnvironment env) {
        String[] profiles = env.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            return "<none>";
        }
        return Arrays.stream(profiles).sorted().reduce((a, b) -> a + "," + b).orElse("<none>");
    }

    private static String extractEnvHint(String desc) {
        Matcher matcher = ENV_HINT_PATTERN.matcher(desc);
        return matcher.find() ? matcher.group(1) : null;
    }
}
