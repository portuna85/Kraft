package com.kraft.lotto.infra.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.ConfigurableEnvironment;

class JdbcConnectivityValidator {

    private static final Pattern JDBC_HOST_PATTERN =
            Pattern.compile("^jdbc:[a-zA-Z0-9]+://([A-Za-z0-9._-]+)");
    private static final Pattern JDBC_ENDPOINT_PATTERN =
            Pattern.compile("^jdbc:[a-zA-Z0-9]+://([A-Za-z0-9._-]+)(?::(\\d+))?");

    private JdbcConnectivityValidator() {
    }

    static void validate(ConfigurableEnvironment env, List<String> problems) {
        String jdbcUrl = RequiredConfigValidator.safeGet(env, "spring.datasource.url");
        if (jdbcUrl == null || jdbcUrl.isBlank() || jdbcUrl.contains("${")) {
            return;
        }
        addHostResolutionProblem(jdbcUrl, problems);
        addTcpReachabilityProblem(env, jdbcUrl, problems);
    }

    private static void addHostResolutionProblem(String jdbcUrl, List<String> problems) {
        String host = extractJdbcHost(jdbcUrl);
        if (host == null || isHostResolvable(host)) {
            return;
        }
        problems.add(
                "  - [spring.datasource.url] DB host is not resolvable by DNS: '" + host + "'" + System.lineSeparator()
                        + "      - For local runtime, set KRAFT_DB_LOCAL_HOST=localhost or adjust KRAFT_DB_URL" + System.lineSeparator()
                        + "      - To skip host rewrite, set KRAFT_DB_HOST_REWRITE=false"
        );
    }

    private static void addTcpReachabilityProblem(ConfigurableEnvironment env, String jdbcUrl, List<String> problems) {
        JdbcEndpoint endpoint = extractJdbcEndpoint(jdbcUrl);
        boolean checkEnabled = Boolean.parseBoolean(env.getProperty("kraft.db.connectivity-check.enabled", "true"));
        int timeoutMs = Integer.parseInt(env.getProperty("kraft.db.connectivity-check.timeout-ms", "1500"));
        if (!checkEnabled || endpoint == null || isTcpReachable(endpoint.host(), endpoint.port(), timeoutMs)) {
            return;
        }
        problems.add(
                "  - [spring.datasource.url] DB endpoint is not reachable: "
                        + endpoint.host() + ":" + endpoint.port() + System.lineSeparator()
                        + "      - Ensure DB is running and reachable from this host" + System.lineSeparator()
                        + "      - For local docker-compose, start DB first: docker compose up -d" + System.lineSeparator()
                        + "      - To skip this precheck, set kraft.db.connectivity-check.enabled=false"
        );
    }

    static String extractJdbcHost(String jdbcUrl) {
        Matcher m = JDBC_HOST_PATTERN.matcher(jdbcUrl);
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

    record JdbcEndpoint(String host, int port) {
    }
}
