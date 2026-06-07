package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpsCollectionController 단위 테스트")
class OpsCollectionControllerTest {

    LottoCollectionCommandService commandService;
    OpsCollectionController controller;

    @BeforeEach
    void setUp() {
        commandService = mock(LottoCollectionCommandService.class);
        controller = new OpsCollectionController(
                commandService,
                mock(OpsCollectionFacade.class),
                mock(KraftSecurityProperties.class)
        );
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
