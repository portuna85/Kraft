package com.kraft.lotto.feature.winningnumber.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WinningStoreEntity")
class WinningStoreEntityTest {

    @Test
    @DisplayName("모든 게터가 생성자에서 설정된 값을 올바르게 반환한다")
    void gettersReturnConstructedValues() {
        LocalDateTime collectedAt = LocalDateTime.of(2026, 6, 6, 20, 0);
        WinningStoreEntity entity = new WinningStoreEntity(1227, 1, "행운복권방", "서울 강남구", 2, collectedAt);

        assertThat(entity.getRound()).isEqualTo(1227);
        assertThat(entity.getGrade()).isEqualTo(1);
        assertThat(entity.getName()).isEqualTo("행운복권방");
        assertThat(entity.getAddress()).isEqualTo("서울 강남구");
        assertThat(entity.getWinCount()).isEqualTo(2);
        assertThat(entity.getCollectedAt()).isEqualTo(collectedAt);
    }

    @Test
    @DisplayName("toDomain이 동일한 값을 갖는 도메인 객체를 반환한다")
    void toDomainReturnsDomainObjectWithSameValues() {
        WinningStoreEntity entity = new WinningStoreEntity(
                1227, 2, "미래복권방", "부산 해운대구", 1, LocalDateTime.now());

        var domain = entity.toDomain();

        assertThat(domain.round()).isEqualTo(1227);
        assertThat(domain.grade()).isEqualTo(2);
        assertThat(domain.name()).isEqualTo("미래복권방");
        assertThat(domain.address()).isEqualTo("부산 해운대구");
        assertThat(domain.winCount()).isEqualTo(1);
    }
}
