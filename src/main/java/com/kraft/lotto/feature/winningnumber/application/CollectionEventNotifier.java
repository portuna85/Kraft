package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import org.springframework.context.ApplicationEventPublisher;

final class CollectionEventNotifier {

    private final ApplicationEventPublisher eventPublisher;

    CollectionEventNotifier(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    void publishCollected(CollectResponse response) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(WinningNumbersCollectedEvent.of(
                response.collected(), response.updated(), response.skipped(), response.failed()
        ));
    }
}
