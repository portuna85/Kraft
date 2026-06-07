package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpsCollectionController 단위 테스트")
class OpsCollectionControllerTest {

    LottoCollectionCommandService commandService;
    WinningStoreCollector storeCollector;
    OpsCollectionController controller;

    @BeforeEach
    void setUp() {
        commandService = mock(LottoCollectionCommandService.class);
        storeCollector = mock(WinningStoreCollector.class);
        controller = new OpsCollectionController(
                commandService,
                mock(OpsCollectionFacade.class),
                storeCollector,
                mock(KraftSecurityProperties.class)
        );
    }

    @Test
    @DisplayName("collectStores는 판매점 수집 결과를 Map으로 반환한다")
    void collectStoresReturnsSavedFlag() {
        when(storeCollector.collectStores(1227)).thenReturn(true);

        var result = controller.collectStores(1227);

        assertThat(result.get("round")).isEqualTo(1227);
        assertThat(result.get("saved")).isEqualTo(true);
    }

    @Test
    @DisplayName("refreshRound는 재수집 결과를 반환한다")
    void refreshRoundReturnsCollectResponse() {
        CollectResponse expected = CollectResponse.ofInserted(1, 1227);
        when(commandService.collectOneRefresh(1227)).thenReturn(expected);

        CollectResponse result = controller.refreshRound(1227);

        assertThat(result).isEqualTo(expected);
    }
}
