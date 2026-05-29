package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JDBC 연결 검증기 테스트")
class RequiredConfigValidatorJdbcTest {

    @Test
    @DisplayName("기본 포트 3306 사용")
    void extractsJdbcEndpointWithDefaultPort() {
        var endpoint = JdbcConnectivityValidator.extractJdbcEndpoint("jdbc:mariadb://localhost/kraft_lotto");
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.host()).isEqualTo("localhost");
        assertThat(endpoint.port()).isEqualTo(3306);
    }

    @Test
    @DisplayName("명시적 포트 사용")
    void extractsJdbcEndpointWithExplicitPort() {
        var endpoint = JdbcConnectivityValidator.extractJdbcEndpoint("jdbc:mariadb://db.internal:3307/kraft_lotto");
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.host()).isEqualTo("db.internal");
        assertThat(endpoint.port()).isEqualTo(3307);
    }
}
