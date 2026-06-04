package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
class LottoSingleDrawCollector {

    private static final Logger log = LoggerFactory.getLogger(LottoSingleDrawCollector.class);

    private final LottoApiClient lottoApiClient;
    private final WinningNumberRepository winningNumberRepository;
    private final WinningNumberPersister persister;
    private final LottoFetchLogRepository fetchLogRepository;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    LottoSingleDrawCollector(LottoApiClient lottoApiClient,
                             WinningNumberRepository winningNumberRepository,
                             WinningNumberPersister persister,
                             LottoFetchLogRepository fetchLogRepository,
                             Clock clock) {
        this(lottoApiClient, winningNumberRepository, persister, fetchLogRepository, clock,
                new SimpleMeterRegistry());
    }

    LottoSingleDrawCollector(LottoApiClient lottoApiClient,
                             WinningNumberRepository winningNumberRepository,
                             WinningNumberPersister persister,
                             LottoFetchLogRepository fetchLogRepository,
                             Clock clock,
                             MeterRegistry meterRegistry) {
        this.lottoApiClient = lottoApiClient;
        this.winningNumberRepository = winningNumberRepository;
        this.persister = persister;
        this.fetchLogRepository = fetchLogRepository;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    CollectResponse collectOne(int drwNo, boolean refresh) {
        return collectOne(drwNo, refresh, null);
    }

    CollectResponse collectOne(int drwNo, boolean refresh, Integer latestRoundHint) {
        int latestRoundBefore = resolveLatestRound(latestRoundHint);
        if (!refresh && winningNumberRepository.existsByRound(drwNo)) {
            saveLog(drwNo, LottoFetchStatus.SKIPPED, "already collected round", null, null);
            recordOutcome("skipped");
            return CollectResponse.ofSkipped(1, Math.max(latestRoundBefore, drwNo));
        }
        long startedNanos = System.nanoTime();
        try {
            return collectFetchedRound(drwNo, latestRoundBefore);
        } catch (LottoApiClientException ex) {
            log.warn("lotto draw collect failed: drwNo={}", drwNo, ex);
            saveLog(
                    drwNo,
                    LottoFetchStatus.FAILED,
                    FetchFailureReasonSupport.normalizeFailureMessage(ex),
                    ex.getResponseCode(),
                    ex.getRawResponse()
            );
            recordOutcome("failed");
            return CollectResponse.ofFailed(List.of(drwNo), latestRoundBefore, false);
        } catch (RuntimeException ex) {
            log.warn("lotto draw collect failed: drwNo={}", drwNo, ex);
            saveLog(
                    drwNo,
                    LottoFetchStatus.FAILED,
                    FetchFailureReasonSupport.normalizeFailureMessage(ex.getMessage()),
                    null,
                    null
            );
            recordOutcome("failed");
            return CollectResponse.ofFailed(List.of(drwNo), latestRoundBefore, false);
        } finally {
            meterRegistry.timer("kraft.collect.fetch.latency")
                    .record(System.nanoTime() - startedNanos, TimeUnit.NANOSECONDS);
        }
    }

    private int resolveLatestRound(Integer latestRoundHint) {
        return latestRoundHint != null ? Math.max(0, latestRoundHint) : winningNumberRepository.findMaxRound().orElse(0);
    }

    private CollectResponse collectFetchedRound(int drwNo, int latestRoundBefore) {
        Optional<WinningNumber> fetched = lottoApiClient.fetch(drwNo);
        if (fetched.isEmpty()) {
            saveLog(drwNo, LottoFetchStatus.NOT_DRAWN, "round not drawn yet", null, null);
            recordOutcome("not_drawn");
            return CollectResponse.ofNotDrawn(latestRoundBefore);
        }

        WinningNumber winningNumber = fetched.get();
        UpsertOutcome outcome = persister.upsert(winningNumber);
        saveOutcomeLog(drwNo, outcome, winningNumber.rawJson());
        recordOutcome(outcome.name().toLowerCase());
        return toCollectResponse(drwNo, outcome, latestRoundBefore);
    }

    private void recordOutcome(String result) {
        meterRegistry.counter("kraft.collect.outcome", "result", result).increment();
    }

    private void saveOutcomeLog(int drwNo, UpsertOutcome outcome, String rawJson) {
        String message = switch (outcome) {
            case INSERTED -> "inserted";
            case UPDATED -> "updated";
            case UNCHANGED -> "unchanged";
            case FAILED -> "failed";
        };
        LottoFetchStatus status = outcome == UpsertOutcome.FAILED ? LottoFetchStatus.FAILED : LottoFetchStatus.SUCCESS;
        saveLog(drwNo, status, message, null, rawJson);
    }

    private CollectResponse toCollectResponse(int drwNo, UpsertOutcome outcome, int latestRoundBefore) {
        int latestRound = outcome == UpsertOutcome.FAILED
                ? latestRoundBefore
                : Math.max(latestRoundBefore, drwNo);
        return switch (outcome) {
            case INSERTED -> CollectResponse.ofInserted(1, latestRound);
            case UPDATED -> CollectResponse.ofUpdated(1, latestRound);
            case UNCHANGED -> CollectResponse.ofSkipped(1, latestRound);
            case FAILED -> CollectResponse.ofFailed(List.of(drwNo), latestRound, false);
        };
    }

    private void saveLog(int drwNo, LottoFetchStatus status, String message, Integer responseCode, String rawResponse) {
        String rawResponseToSave = status == LottoFetchStatus.SUCCESS ? null : rawResponse;
        fetchLogRepository.save(new LottoFetchLogEntity(
                drwNo,
                status == LottoFetchStatus.SUCCESS ? drwNo : null,
                status,
                message,
                responseCode,
                rawResponseToSave,
                LocalDateTime.now(clock)
        ));
    }
}
