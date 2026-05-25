package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("Dotenv 환경 포스트 프로세서")
class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("설정된 안전 제한보다 큰 .env 파일은 무시한다")
    void skipsOversizedDotenvFile() throws Exception {
        Path file = tempDir.resolve(".env");
        byte[] content = new byte[(int) DotenvEnvironmentPostProcessor.MAX_DOTENV_BYTES + 1];
        Files.write(file, content);

        var parsed = new DotenvEnvironmentPostProcessor().parse(file);

        assertThat(parsed).isEmpty();
    }

    @Test
    @DisplayName("지정된 .env 경로를 환경변수로 전달할 수 있다")
    void loadsExplicitDotenvPathFromEnvironmentVariable() throws Exception {
        Path file = tempDir.resolve("custom.env");
        Files.writeString(file, "KRAFT_DB_USER=alice\n", StandardCharsets.UTF_8);

        MockEnvironment env = new MockEnvironment();
        env.setProperty("KRAFT_DOTENV_PATH", file.toString());

        new DotenvEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("KRAFT_DB_USER")).isEqualTo("alice");
    }

    @Test
    @DisplayName("dotenv 로딩을 비활성화하면 파일을 읽지 않는다")
    void disablesDotenvLoadingWhenRequested() throws Exception {
        Path file = tempDir.resolve(".env");
        Files.writeString(file, "KRAFT_DB_USER=alice\n", StandardCharsets.UTF_8);

        MockEnvironment env = new MockEnvironment();
        env.setProperty("KRAFT_DOTENV_ENABLED", "false");
        env.setProperty("KRAFT_DOTENV_PATH", file.toString());

        new DotenvEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("KRAFT_DB_USER")).isNull();
    }
}
