package com.kraft.common.config;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * application-prod.yml은 실행 시점에만 값이 반영되는 순수 설정 파일이라 Spring 컨텍스트를
 * prod 프로파일로 띄우지 않고도(데이터소스·외부 URL 등 운영 전용 환경변수 없이) 정적으로
 * 검증할 수 있다 — BE-18: 세션 쿠키 속성이 명시적으로 강화돼 있는지 회귀 확인.
 */
@DisplayName("운영 세션 쿠키 설정 테스트")
class ProdSessionCookieConfigTest {

    @Test
    @DisplayName("application-prod.yml은 세션 쿠키를 secure·same-site=strict·http-only로 강화한다")
    void applicationProdYml_hardensSessionCookieAttributes() {
        Map<String, Object> yaml = loadYaml("/application-prod.yml");

        Map<?, ?> server = (Map<?, ?>) yaml.get("server");
        Map<?, ?> servlet = (Map<?, ?>) server.get("servlet");
        Map<?, ?> session = (Map<?, ?>) servlet.get("session");
        Map<?, ?> cookie = (Map<?, ?>) session.get("cookie");

        assertThat(cookie.get("secure")).isEqualTo(true);
        assertThat(cookie.get("same-site")).isEqualTo("strict");
        assertThat(cookie.get("http-only")).isEqualTo(true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String classpathResource) {
        try (InputStream in = ProdSessionCookieConfigTest.class.getResourceAsStream(classpathResource)) {
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + classpathResource, e);
        }
    }
}
