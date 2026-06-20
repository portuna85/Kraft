package com.kraft.common.config;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProdEnvironmentValidator 단위 테스트")
class ProdEnvironmentValidatorTest {

    @Test
    @DisplayName("필수 환경변수가 모두 있으면 누락 목록이 비어야 한다")
    void missingRequiredVariables_returnsEmptyWhenAllPresent() throws Exception {
        ProdEnvironmentValidator validator = validator(
                "https://kraft.example.com",
                "https://example.com/lotto?round=%d",
                "secret",
                "ops-token"
        );

        assertThat(validator.missingRequiredVariables()).isEmpty();
    }

    @Test
    @DisplayName("비어 있는 prod 필수값만 누락 목록에 포함해야 한다")
    void missingRequiredVariables_returnsOnlyMissingKeys() throws Exception {
        ProdEnvironmentValidator validator = validator(
                "",
                " ",
                null,
                ""
        );

        assertThat(validator.missingRequiredVariables()).containsExactly(
                "KRAFT_PUBLIC_BASE_URL",
                "KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE",
                "KRAFT_REVALIDATE_SECRET",
                "KRAFT_OPS_TOKEN"
        );
    }

    private static ProdEnvironmentValidator validator(String publicBaseUrl,
                                                      String externalUrlTemplate,
                                                      String revalidateSecret,
                                                      String opsToken) throws Exception {
        ProdEnvironmentValidator validator = new ProdEnvironmentValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                new ExternalLottoProperties(externalUrlTemplate, "0 30 21 * * SAT"),
                new RevalidateProperties(revalidateSecret, "http://web:3000"),
                new OpsProperties(opsToken)
        );
        Field publicBaseUrlField = ProdEnvironmentValidator.class.getDeclaredField("publicBaseUrl");
        publicBaseUrlField.setAccessible(true);
        publicBaseUrlField.set(validator, publicBaseUrl);
        return validator;
    }
}
