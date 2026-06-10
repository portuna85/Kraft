package com.kraft.lotto.infra.fcm;

import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import java.util.List;

public interface FcmService {

    void subscribeToDrawResult(String token);

    void unsubscribeFromDrawResult(String token);

    void sendDrawResult(WinningNumberDto dto);

    /**
     * Unsubscribe a batch of stale tokens from all managed topics.
     */
    void unsubscribeBatch(List<String> tokens);
}
