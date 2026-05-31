package com.kraft.lotto.feature.statistics.application;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FrequencySummaryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Spring-managed service constructor validates required wiring")
public class WinningStatisticsService {

    private final WinningStatisticsCacheService cacheService;
    private final CacheManager cacheManager;

    @Autowired
    public WinningStatisticsService(WinningStatisticsCacheService cacheService,
                                    ObjectProvider<CacheManager> cacheManagerProvider) {
        this.cacheService = cacheService;
        this.cacheManager = cacheManagerProvider.getIfAvailable();
    }

    WinningStatisticsService(WinningStatisticsCacheService cacheService) {
        this.cacheService = cacheService;
        this.cacheManager = null;
    }

    WinningStatisticsService(WinningStatisticsCacheService cacheService, CacheManager cacheManager) {
        this.cacheService = cacheService;
        this.cacheManager = cacheManager;
    }

    public List<NumberFrequencyDto> frequency() {
        return cacheService.frequency();
    }

    public CombinationPrizeHistoryDto combinationPrizeHistory(List<Integer> numbers) {
        return cacheService.combinationPrizeHistory(numbers);
    }

    public List<NumberFrequencyDto> frequencyForPeriod(int rounds) {
        return cacheService.frequencyForPeriod(rounds);
    }

    public PatternStatDto patternStats() {
        return cacheService.patternStats();
    }

    public List<CompanionNumberDto> companionNumbers(int target) {
        return cacheService.companionNumbers(target);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "winningFrequencySummary", sync = true)
    public FrequencySummaryDto frequencySummary() {
        List<NumberFrequencyDto> frequencies = cacheService.frequency();
        List<Integer> lowSixNumbers = frequencies.stream()
                .sorted(Comparator.comparingLong(NumberFrequencyDto::count).thenComparingInt(NumberFrequencyDto::number))
                .limit(6)
                .map(NumberFrequencyDto::number)
                .sorted()
                .toList();
        CombinationPrizeHistoryDto lowSixHistory = cacheService.combinationPrizeHistory(lowSixNumbers);
        return new FrequencySummaryDto(frequencies, lowSixHistory);
    }

    @Async
    @EventListener
    public void evictCachesOnCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            return;
        }
        evictAll("winningNumberFrequency");
        evictAll("combinationPrizeHistory");
        evictAll("winningFrequencySummary");
        cacheService.refreshFrequencySummary();
    }

    private void evictAll(String cacheName) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
