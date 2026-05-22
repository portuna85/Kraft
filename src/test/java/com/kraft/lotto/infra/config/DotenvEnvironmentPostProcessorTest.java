package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
