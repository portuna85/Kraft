package com.kraft.lotto.infra.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates required runtime configuration before the app starts.
 */
public class RequiredConfigValidator implements EnvironmentPostProcessor, Ordered {

    public static final int ORDER = DotenvEnvironmentPostProcessor.ORDER + 100;
    private static final String NL = System.lineSeparator();

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

    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("^jdbc:[a-zA-Z0-9]+://([A-Za-z0-9._-]+)");
    private static final Pattern JDBC_ENDPOINT_PATTERN =
            Pattern.compile("^jdbc:[a-zA-Z0-9]+://([A-Za-z0-9._-]+)(?::(\\d+))?");
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
        validateJdbcConnectivity(env, problems);

        addProdOperationalConfigProblems(env, problems);
        addProfilePolicyProblems(env, problems);

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

    private static String format(String key, String desc, String reason) {
        StringBuilder line = new StringBuilder("  - [").append(key).append("] ").append(desc).append(" => ").append(reason);
        String envHint = extractEnvHint(desc);
        if (envHint != null) {
            line.append(NL).append("      - action: set ").append(envHint);
        }
        return line.toString();
    }

    private static String safeGet(ConfigurableEnvironment env, String key) {
        try {
            return env.getProperty(key);
        } catch (RuntimeException ex) {
            return null;
        }
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

    private static void validateJdbcConnectivity(ConfigurableEnvironment env, List<String> problems) {
        String jdbcUrl = safeGet(env, "spring.datasource.url");
        if (jdbcUrl == null || jdbcUrl.isBlank() || jdbcUrl.contains("${")) {
            return;
        }
        addJdbcHostResolutionProblem(jdbcUrl, problems);
        addJdbcTcpReachabilityProblem(env, jdbcUrl, problems);
    }

    private static void addJdbcHostResolutionProblem(String jdbcUrl, List<String> problems) {
        String host = extractJdbcHost(jdbcUrl);
        if (host == null || isHostResolvable(host)) {
            return;
        }
        problems.add(
                "  - [spring.datasource.url] DB host is not resolvable by DNS: '" + host + "'" + NL
                        + "      - For local runtime, set KRAFT_DB_LOCAL_HOST=localhost or adjust KRAFT_DB_URL" + NL
                        + "      - To skip host rewrite, set KRAFT_DB_HOST_REWRITE=false"
        );
    }

    private static void addJdbcTcpReachabilityProblem(ConfigurableEnvironment env, String jdbcUrl, List<String> problems) {
        JdbcEndpoint endpoint = extractJdbcEndpoint(jdbcUrl);
        boolean checkEnabled = Boolean.parseBoolean(env.getProperty("kraft.db.connectivity-check.enabled", "true"));
        int timeoutMs = Integer.parseInt(env.getProperty("kraft.db.connectivity-check.timeout-ms", "1500"));
        if (!checkEnabled || endpoint == null || isTcpReachable(endpoint.host(), endpoint.port(), timeoutMs)) {
            return;
        }
        problems.add(
                "  - [spring.datasource.url] DB endpoint is not reachable: " + endpoint.host() + ":" + endpoint.port() + NL
                        + "      - Ensure DB is running and reachable from this host" + NL
                        + "      - For local docker-compose, start DB first: docker compose up -d" + NL
                        + "      - To skip this precheck, set kraft.db.connectivity-check.enabled=false"
        );
    }

    static void addProdOperationalConfigProblems(ConfigurableEnvironment env, List<String> problems) {
        if (!env.matchesProfiles("prod")) {
            return;
        }
        requireNonBlank(env, problems, "kraft.api.url", "External API URL (env: KRAFT_API_URL)");
        requireNonBlank(env, problems, "kraft.security.ops.required-token",
                "Ops API token (env: KRAFT_SECURITY_OPS_REQUIRED_TOKEN)");
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
                "Recommend decade threshold (env: KRAFT_RECOMMEND_RULE_DECADE_THRESHOLD)", 2, 6);

        String apiClient = safeGet(env, "kraft.api.client");
        Set<String> allowedProdClients = Set.of("real", "dhlottery", "smok");
        if (apiClient == null || apiClient.isBlank()
                || !allowedProdClients.contains(apiClient.trim().toLowerCase())) {
            problems.add(format(
                    "kraft.api.client",
                    "Lotto API client mode (env: KRAFT_API_CLIENT)",
                    "prod profile requires one of: " + allowedProdClients
            ));
        }
    }

    private static void requireIntInRange(ConfigurableEnvironment env,
                                          List<String> problems,
                                          String key,
                                          String desc,
                                          int min,
                                          int max) {
        String value = safeGet(env, key);
        if (value == null || value.isBlank()) {
            problems.add(format(key, desc, "required in prod profile but blank"));
            return;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                problems.add(format(key, desc, "must be between " + min + " and " + max + " (actual: " + parsed + ")"));
            }
        } catch (NumberFormatException ex) {
            problems.add(format(key, desc, "must be an integer (actual: " + value + ")"));
        }
    }

    private static void requireNonBlank(ConfigurableEnvironment env,
                                        List<String> problems,
                                        String key,
                                        String desc) {
        String value = safeGet(env, key);
        if (value == null || value.isBlank()) {
            problems.add(format(key, desc, "required in prod profile but blank"));
        }
    }

    static void addProfilePolicyProblems(ConfigurableEnvironment env, List<String> problems) {
        boolean inContainer = Boolean.parseBoolean(env.getProperty("KRAFT_IN_CONTAINER", "false"));
        String kraftEnv = env.getProperty("KRAFT_ENV", "").trim().toLowerCase();
        String[] activeProfiles = env.getActiveProfiles();
        String active = activeProfiles.length == 0 ? "<none>" : String.join(",", activeProfiles);
        boolean activeLocal = env.matchesProfiles("local");
        boolean activeProd = env.matchesProfiles("prod");

        if (inContainer) {
            if (!activeProd) {
                problems.add(format(
                        "spring.profiles.active",
                        "active profile",
                        "KRAFT_IN_CONTAINER=true requires prod profile (current: " + active + ")"
                ));
            }
        } else if (!activeLocal) {
            problems.add(format(
                    "spring.profiles.active",
                    "active profile",
                    "local runtime requires local profile (current: " + active + ")"
            ));
        }

        if (kraftEnv.isBlank()) {
            return;
        }

        if (!"local".equals(kraftEnv) && !"prod".equals(kraftEnv)) {
            problems.add(format(
                    "KRAFT_ENV",
                    "environment discriminator (allowed: local|prod)",
                    "invalid value: " + kraftEnv
            ));
            return;
        }

        if ("local".equals(kraftEnv) && !activeLocal) {
            problems.add(format(
                    "KRAFT_ENV",
                    "environment discriminator",
                    "KRAFT_ENV=local requires local profile (current: " + active + ")"
            ));
        } else if ("prod".equals(kraftEnv) && !activeProd) {
            problems.add(format(
                    "KRAFT_ENV",
                    "environment discriminator",
                    "KRAFT_ENV=prod requires prod profile (current: " + active + ")"
            ));
        }
    }

    static String extractJdbcHost(String jdbcUrl) {
        Matcher m = JDBC_URL_PATTERN.matcher(jdbcUrl);
        return m.find() ? m.group(1) : null;
    }

    static JdbcEndpoint extractJdbcEndpoint(String jdbcUrl) {
        Matcher m = JDBC_ENDPOINT_PATTERN.matcher(jdbcUrl);
        if (!m.find()) {
            return null;
        }
        String host = m.group(1);
        String portRaw = m.group(2);
        int port = (portRaw == null || portRaw.isBlank()) ? 3306 : Integer.parseInt(portRaw);
        return new JdbcEndpoint(host, port);
    }

    private static boolean isHostResolvable(String host) {
        if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.") || "::1".equals(host)) {
            return true;
        }
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private static boolean isTcpReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.max(timeoutMs, 100));
            return true;
        } catch (Exception ex) {
            return false;
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

    record JdbcEndpoint(String host, int port) {
    }
}
