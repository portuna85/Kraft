package com.kraft.lotto.feature.winningnumber.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WinningStoreDto")
class WinningStoreDtoTest {

    @Test
    @DisplayName("WinningStore 도메인으로부터 DTO를 생성한다")
    void fromDomain() {
        WinningStore store = new WinningStore(1226, 1, "거봉마트", "대구 서구 북비산로 310", 1);

        WinningStoreDto dto = WinningStoreDto.from(store);

        assertThat(dto.grade()).isEqualTo(1);
        assertThat(dto.name()).isEqualTo("거봉마트");
        assertThat(dto.address()).isEqualTo("대구 서구 북비산로 310");
        assertThat(dto.winCount()).isEqualTo(1);
        assertThat(dto.naverMapUrl()).startsWith("https://map.naver.com/v5/search/");
        assertThat(dto.naverMapUrl()).contains("%EA%B1%B0%EB%B4%89%EB%A7%88%ED%8A%B8");
    }

    @Test
    @DisplayName("주소가 빈 문자열이어도 naverMapUrl을 생성한다")
    void fromDomainWithEmptyAddress() {
        WinningStore store = new WinningStore(1226, 2, "온라인판매점", "", 3);

        WinningStoreDto dto = WinningStoreDto.from(store);

        assertThat(dto.naverMapUrl()).isNotBlank();
        assertThat(dto.winCount()).isEqualTo(3);
    }
}
