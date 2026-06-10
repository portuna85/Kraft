package com.kraft.lotto.infra.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.TopicManagementResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import com.kraft.lotto.infra.config.KraftFcmProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "kraft.fcm.enabled", havingValue = "true")
public class FirebaseFcmService implements FcmService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmService.class);
    private static final int TOPIC_BATCH_LIMIT = 1000;

    private final FirebaseMessaging messaging;
    private final String drawResultTopic;

    public FirebaseFcmService(FirebaseMessaging messaging, KraftFcmProperties props) {
        this.messaging = messaging;
        this.drawResultTopic = props.getDrawResultTopic();
    }

    @Override
    public void subscribeToDrawResult(String token) {
        try {
            TopicManagementResponse res = messaging.subscribeToTopic(List.of(token), drawResultTopic);
            if (res.getFailureCount() > 0) {
                log.warn("fcm subscribe partial failure token={} errors={}", token, res.getErrors());
            }
        } catch (FirebaseMessagingException e) {
            log.warn("fcm subscribe failed token={} code={}", token, e.getMessagingErrorCode(), e);
        }
    }

    @Override
    public void unsubscribeFromDrawResult(String token) {
        try {
            TopicManagementResponse res = messaging.unsubscribeFromTopic(List.of(token), drawResultTopic);
            if (res.getFailureCount() > 0) {
                log.warn("fcm unsubscribe partial failure token={} errors={}", token, res.getErrors());
            }
        } catch (FirebaseMessagingException e) {
            log.warn("fcm unsubscribe failed token={} code={}", token, e.getMessagingErrorCode(), e);
        }
    }

    @Override
    public void sendDrawResult(WinningNumberDto dto) {
        String numbers = dto.numbers().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        String title = String.format("제 %d회 로또 추첨 결과", dto.round());
        String body  = numbers + " + " + dto.bonusNumber();

        Message msg = Message.builder()
                .setTopic(drawResultTopic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("round", String.valueOf(dto.round()))
                .putData("type", "draw_result")
                .build();
        try {
            String messageId = messaging.send(msg);
            log.info("fcm draw-result sent round={} messageId={}", dto.round(), messageId);
        } catch (FirebaseMessagingException e) {
            log.error("fcm draw-result send failed round={} code={}", dto.round(), e.getMessagingErrorCode(), e);
        }
    }

    @Override
    public void unsubscribeBatch(List<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        // FCM topic management allows max 1000 tokens per call
        int from = 0;
        while (from < tokens.size()) {
            List<String> batch = tokens.subList(from, Math.min(from + TOPIC_BATCH_LIMIT, tokens.size()));
            try {
                TopicManagementResponse res = messaging.unsubscribeFromTopic(batch, drawResultTopic);
                if (res.getFailureCount() > 0) {
                    log.warn("fcm batch unsubscribe partial failure size={} failures={}",
                            batch.size(), res.getFailureCount());
                }
            } catch (FirebaseMessagingException e) {
                log.warn("fcm batch unsubscribe failed size={} code={}", batch.size(), e.getMessagingErrorCode(), e);
            }
            from += TOPIC_BATCH_LIMIT;
        }
    }
}
