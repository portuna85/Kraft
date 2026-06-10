package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.KraftLottoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@DisplayName("운영 프로파일 기본값 테스트")
class HistoryInitProdDefaultTest {

    @Test
    @DisplayName("운영 프로파일에서 이력 초기화은 기본적으로 비활성화된다")
    void prodDefaultDisablesHistoryInit() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(KraftLottoApplication.class)
                .profiles("prod")
                .run(
                        "--spring.main.web-application-type=none",
                        "--spring.datasource.url=jdbc:h2:mem:history-init-prod-default;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.flyway.enabled=true",
                        "--spring.jpa.hibernate.ddl-auto=none",
                        "--spring.sql.init.mode=never",
                        "--kraft.db.connectivity-check.enabled=false"
                )) {
            assertThat(context.getEnvironment().getProperty("kraft.history-init.enabled")).isEqualTo("false");
        }
    }
}
