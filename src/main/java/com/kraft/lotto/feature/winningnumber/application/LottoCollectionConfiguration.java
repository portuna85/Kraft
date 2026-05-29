package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.infra.config.KraftApiProperties;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LottoCollectionConfiguration {

    @Bean
    WinningNumberUpsertExecutor winningNumberUpsertExecutor(WinningNumberRepository winningNumberRepository) {
        return new WinningNumberUpsertExecutor(winningNumberRepository, Clock.systemDefaultZone());
    }

    @Bean
    LottoSingleDrawCollector lottoSingleDrawCollector(LottoApiClient lottoApiClient,
                                                      WinningNumberRepository winningNumberRepository,
                                                      WinningNumberPersister persister,
                                                      LottoFetchLogRepository fetchLogRepository) {
        return new LottoSingleDrawCollector(
                lottoApiClient,
                winningNumberRepository,
                persister,
                fetchLogRepository,
                Clock.systemDefaultZone()
        );
    }

    @Bean
    LottoRangeCollector lottoRangeCollector(LottoSingleDrawCollector singleDrawCollector,
                                            WinningNumberRepository winningNumberRepository,
                                            ObjectProvider<MeterRegistry> meterRegistryProvider,
                                            KraftApiProperties properties) {
        return new LottoRangeCollector(
                singleDrawCollector,
                winningNumberRepository,
                properties.backfillDelayMs(),
                meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new)
        );
    }

    @Bean
    LottoCollectionCommandService lottoCollectionCommandService(WinningNumberRepository winningNumberRepository,
                                                                LottoSingleDrawCollector singleDrawCollector,
                                                                LottoRangeCollector rangeCollector,
                                                                ApplicationEventPublisher eventPublisher,
                                                                KraftApiProperties properties,
                                                                KraftCollectProperties collectProperties) {
        return new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                properties.backfillDelayMs(),
                collectProperties.maxPerRun(),
                collectProperties.maxHistoryCollect(),
                collectProperties.stopOnFailure()
        );
    }
}
