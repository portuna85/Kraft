package com.kraft.lotto.feature.push.application;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import com.kraft.lotto.infra.fcm.FcmService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kraft.fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DrawResultPushListener {

    private static final Logger log = LoggerFactory.getLogger(DrawResultPushListener.class);

    private final FcmService fcmService;
    private final WinningNumberQueryService queryService;

    @EventListener
    @Async
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            return;
        }
        Optional<WinningNumberDto> latest = queryService.findLatest();
        if (latest.isEmpty()) {
            log.warn("draw result push skipped — no winning number found after collection");
            return;
        }
        WinningNumberDto dto = latest.get();
        log.info("draw result push start round={}", dto.round());
        fcmService.sendDrawResult(dto);
    }
}
