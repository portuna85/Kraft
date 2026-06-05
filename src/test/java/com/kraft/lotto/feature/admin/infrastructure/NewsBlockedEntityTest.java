package com.kraft.lotto.feature.admin.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("뉴스 차단 엔티티 빌더/게터 테스트")
class NewsBlockedEntityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    @DisplayName("NewsBlockedDomainEntity — 빌더로 생성 후 필드를 반환한다")
    void newsBlockedDomainEntityBuilderAndGetters() {
        NewsBlockedDomainEntity entity = NewsBlockedDomainEntity.builder()
                .domain("spam.example.com")
                .reason("스팸 사이트")
                .createdBy("admin@example.com")
                .createdAt(NOW)
                .build();

        assertThat(entity.getDomain()).isEqualTo("spam.example.com");
        assertThat(entity.getReason()).isEqualTo("스팸 사이트");
        assertThat(entity.getCreatedBy()).isEqualTo("admin@example.com");
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getId()).isNull();
    }

    @Test
    @DisplayName("NewsBlockedKeywordEntity — 빌더로 생성 후 필드를 반환한다")
    void newsBlockedKeywordEntityBuilderAndGetters() {
        NewsBlockedKeywordEntity entity = NewsBlockedKeywordEntity.builder()
                .keyword("아파트 로또")
                .reason("부동산 관련 키워드")
                .createdBy("admin@example.com")
                .createdAt(NOW)
                .build();

        assertThat(entity.getKeyword()).isEqualTo("아파트 로또");
        assertThat(entity.getReason()).isEqualTo("부동산 관련 키워드");
        assertThat(entity.getCreatedBy()).isEqualTo("admin@example.com");
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getId()).isNull();
    }
}
