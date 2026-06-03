package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.DataChangeLogDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class LottoFetchLogQueryService {

    private final LottoFetchLogRepository fetchLogRepository;
    private final Clock clock;

    @Autowired
    public LottoFetchLogQueryService(LottoFetchLogRepository fetchLogRepository) {
        this(fetchLogRepository, Clock.systemDefaultZone());
    }

    LottoFetchLogQueryService(LottoFetchLogRepository fetchLogRepository, Clock clock) {
        this.fetchLogRepository = fetchLogRepository;
        this.clock = clock;
    }

    public List<FetchFailureReasonDto> summarizeRecentFailureReasons(int limit) {
        return summarizeRecentFailureReasons(limit, null, null, null);
    }

    public List<FetchFailureReasonDto> summarizeRecentFailureReasons(int limit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        List<LottoFetchLogEntity> failedLogs = fetchFailedLogs(limit, drwNoFrom, drwNoTo, reason);
        Map<String, Long> grouped = failedLogs.stream()
                .map(LottoFetchLogEntity::getMessage)
                .map(FetchFailureReasonSupport::extractReason)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return grouped.entrySet().stream()
                .map(e -> new FetchFailureReasonDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(FetchFailureReasonDto::count).reversed()
                        .thenComparing(FetchFailureReasonDto::reason))
                .toList();
    }

    public List<FetchFailureLogDto> listRecentFailures(int limit) {
        return listRecentFailures(limit, null, null, null);
    }

    public List<FetchFailureLogDto> listRecentFailures(int limit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        return fetchFailedLogs(limit, drwNoFrom, drwNoTo, reason).stream()
                .map(LottoFetchLogQueryService::toFetchFailureLogDto)
                .toList();
    }

    public FetchFailureOverviewDto failureOverview(int reasonLimit, int logLimit) {
        return failureOverview(reasonLimit, logLimit, null, null, null);
    }

    public FetchFailureOverviewDto failureOverview(int reasonLimit, int logLimit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        return new FetchFailureOverviewDto(
                LocalDateTime.now(clock),
                reasonLimit,
                logLimit,
                summarizeRecentFailureReasons(reasonLimit, reason, drwNoFrom, drwNoTo),
                listRecentFailures(logLimit, reason, drwNoFrom, drwNoTo)
        );
    }

    public FetchFailureReasonsResponseDto failureReasonsResponse(int limit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        return new FetchFailureReasonsResponseDto(
                LocalDateTime.now(clock),
                limit,
                reason,
                drwNoFrom,
                drwNoTo,
                summarizeRecentFailureReasons(limit, reason, drwNoFrom, drwNoTo)
        );
    }

    public FetchFailureLogsResponseDto failuresResponse(int limit, String reason, Integer drwNoFrom, Integer drwNoTo) {
        return new FetchFailureLogsResponseDto(
                LocalDateTime.now(clock),
                limit,
                reason,
                drwNoFrom,
                drwNoTo,
                listRecentFailures(limit, reason, drwNoFrom, drwNoTo)
        );
    }

    public List<DataChangeLogDto> recentCollectionLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return fetchLogRepository.findRecentAll(PageRequest.of(0, safeLimit)).stream()
                .map(e -> DataChangeLogDto.of(e.getDrwNo(), e.getStatus(), e.getFetchedAt()))
                .toList();
    }

    public FetchLogRetentionStatusDto retentionStatus(boolean enabled,
                                                      int retentionDays,
                                                      int deleteBatchSize,
                                                      String cron,
                                                      String zone) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(Math.max(1, retentionDays));
        long totalLogs = fetchLogRepository.count();
        long purgeEligibleLogs = fetchLogRepository.countByFetchedAtBefore(cutoff);
        return new FetchLogRetentionStatusDto(
                now,
                enabled,
                Math.max(1, retentionDays),
                Math.max(100, deleteBatchSize),
                cron,
                zone,
                cutoff,
                totalLogs,
                purgeEligibleLogs,
                fetchLogRepository.findOldestFetchedAt(),
                fetchLogRepository.findNewestFetchedAt()
        );
    }

    public PagedFailures listRecentFailuresPage(int page, int pageSize, String reason, Integer drwNoFrom, Integer drwNoTo) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(pageSize, 200));
        int fetchSize = safeSize + 1;
        PageRequest pageable = PageRequest.of(safePage, fetchSize);
        String reasonPattern = toReasonFilter(reason);
        List<LottoFetchLogEntity> fetched = fetchLogRepository.findRecentFailedFilteredByReason(
                drwNoFrom,
                drwNoTo,
                reasonPattern,
                pageable
        );

        List<FetchFailureLogDto> rows = fetched.stream()
                .limit(safeSize)
                .map(LottoFetchLogQueryService::toFetchFailureLogDto)
                .toList();

        boolean hasNext = fetched.size() > safeSize;
        return new PagedFailures(rows, safePage, safeSize, hasNext);
    }

    private List<LottoFetchLogEntity> fetchFailedLogs(int limit, Integer drwNoFrom, Integer drwNoTo, String reason) {
        return fetchLogRepository.findRecentFailedFilteredByReason(
                limit,
                drwNoFrom,
                drwNoTo,
                toReasonFilter(reason)
        );
    }

    private String toReasonFilter(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim().toLowerCase(Locale.ROOT);
    }

    private static FetchFailureLogDto toFetchFailureLogDto(LottoFetchLogEntity log) {
        return new FetchFailureLogDto(
                log.getId(),
                log.getDrwNo(),
                log.getResponseCode(),
                FetchFailureReasonSupport.extractReason(log.getMessage()),
                FetchFailureReasonSupport.stripReasonPrefix(log.getMessage()),
                log.getFetchedAt()
        );
    }

    public record PagedFailures(
            List<FetchFailureLogDto> rows,
            int page,
            int pageSize,
            boolean hasNext
    ) {
        public PagedFailures {
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }
}
