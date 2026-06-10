package com.kraft.lotto.infra.fcm;

import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "kraft.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFcmService implements FcmService {

    private static final Logger log = LoggerFactory.getLogger(NoOpFcmService.class);

    @Override
    public void subscribeToDrawResult(String token) {
        log.debug("fcm disabled — skip subscribeToDrawResult token={}", token);
    }

    @Override
    public void unsubscribeFromDrawResult(String token) {
        log.debug("fcm disabled — skip unsubscribeFromDrawResult token={}", token);
    }

    @Override
    public void sendDrawResult(WinningNumberDto dto) {
        log.debug("fcm disabled — skip sendDrawResult round={}", dto.round());
    }

    @Override
    public void unsubscribeBatch(List<String> tokens) {
        log.debug("fcm disabled — skip unsubscribeBatch size={}", tokens.size());
    }
}
