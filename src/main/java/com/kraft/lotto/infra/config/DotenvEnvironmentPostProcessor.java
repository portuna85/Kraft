package com.kraft.lotto.infra.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 선택적으로 {@code .env} 파일을 Spring {@link ConfigurableEnvironment} 에 주입한다.
 *
 * <p>기본 원칙:
 * <ol>
 *   <li>실제 OS 환경 변수와 시스템 프로퍼티를 최우선으로 사용한다.</li>
 *   <li>{@code .env} 는 보조 수단이며, 파일이 없거나 비활성화되면 아무 작업도 하지 않는다.</li>
 * </ol>
 *
 * <p>제어 방법:
 * <ul>
 *   <li>{@code KRAFT_DOTENV_ENABLED=false} - 파일 로딩 비활성화.</li>
 *   <li>{@code KRAFT_DOTENV_PATH=/path/to/.env} - 특정 파일을 명시적으로 사용.</li>
 *   <li>미지정 시 현재 작업 디렉터리와 상위 디렉터리에서 {@code .env} 를 탐색한다.</li>
 * </ul>
 *
 * <p>지원 문법:
 * <ul>
 *   <li>{@code KEY=VALUE} (공백 트리밍)</li>
 *   <li>{@code # ...} 주석 / 빈 줄 무시</li>
 *   <li>선택적 {@code export } 접두사 허용</li>
 *   <li>큰따옴표/작은따옴표로 감싼 값의 따옴표 제거</li>
 * </ul>
 */
public class DotenvEnvironmentPostProcessor
        implements EnvironmentPostProcessor, ApplicationListener<ApplicationPreparedEvent>, Ordered {

    /** 가장 먼저 적용해 후속 PropertySource 들이 이 값을 참조 가능하도록 한다. */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    private static final String SOURCE_NAME = "kraftDotenv";
    static final long MAX_DOTENV_BYTES = 1_048_576L;

    private final DeferredLog log = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (isDisabled(env)) {
            log.debug(".env 로딩이 비활성화되어 있습니다.");
            return;
        }
        Path file = locateDotenvFile(env);
        if (file == null) {
            log.debug(".env 파일을 찾지 못했습니다. 환경변수만 사용합니다.");
            return;
        }
        Map<String, Object> parsed = parse(file);
        if (parsed.isEmpty()) {
            log.debug(".env 파일이 비어 있습니다: " + file);
            return;
        }
        // 이미 OS/시스템 환경에 정의된 키는 덮지 않는다.
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : parsed.entrySet()) {
            if (env.getProperty(e.getKey()) == null) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        if (filtered.isEmpty()) {
            log.info(".env 모든 키가 이미 환경에 존재하여 건너뜀: " + file);
            return;
        }
        env.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, filtered));
        log.info(".env 로드 완료: " + file + " (keys=" + filtered.size() + ")");
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        log.replayTo(DotenvEnvironmentPostProcessor.class);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean isDisabled(ConfigurableEnvironment env) {
        return "false".equalsIgnoreCase(env.getProperty("KRAFT_DOTENV_ENABLED", "true").trim());
    }

    /** 현재 작업 디렉터리 → 상위로 한 단계까지 .env 를 탐색한다. */
    private static Path locateDotenvFile(ConfigurableEnvironment env) {
        String explicitPath = env.getProperty("KRAFT_DOTENV_PATH");
        if (explicitPath != null && !explicitPath.isBlank()) {
            Path path = Paths.get(explicitPath.trim());
            return Files.isRegularFile(path) ? path : null;
        }
        Path cwd = Paths.get("").toAbsolutePath();
        Path here = cwd.resolve(".env");
        if (Files.isRegularFile(here)) {
            return here;
        }
        Path parent = cwd.getParent();
        if (parent != null) {
            Path up = parent.resolve(".env");
            if (Files.isRegularFile(up)) {
                return up;
            }
        }
        return null;
    }

    Map<String, Object> parse(Path file) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            long size = Files.size(file);
            if (size > MAX_DOTENV_BYTES) {
                log.warn(".env file is too large and was skipped: " + file + " (bytes=" + size + ")");
                return out;
            }
        } catch (IOException ex) {
            log.warn(".env size check failed: " + file + " — " + ex.getMessage());
            return out;
        }
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                value = stripQuotes(value);
                if (!key.isEmpty()) {
                    out.put(key, value);
                }
            }
        } catch (IOException ex) {
            // .env 읽기 실패는 치명적이지 않으므로 경고만 남긴다.
            // (DeferredLog 는 부트 완료 후 실제 출력)
            log.warn(".env 읽기 실패: " + file + " — " + ex.getMessage());
        }
        return out;
    }

    private static String stripQuotes(String v) {
        if (v.length() >= 2) {
            char a = v.charAt(0);
            char b = v.charAt(v.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }
}
