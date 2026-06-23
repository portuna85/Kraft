package com.kraft.notification;

import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.saved.SavedNumberRepository;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DrawNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DrawNotificationScheduler.class);

    private final EmailSubscriptionRepository subscriptionRepository;
    private final WinningNumberRepository winningNumberRepository;
    private final SavedNumberRepository savedNumberRepository;
    private final LottoNumberCodec codec;
    private final EmailSubscriptionService emailService;

    public DrawNotificationScheduler(EmailSubscriptionRepository subscriptionRepository,
                                     WinningNumberRepository winningNumberRepository,
                                     SavedNumberRepository savedNumberRepository,
                                     LottoNumberCodec codec,
                                     EmailSubscriptionService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.winningNumberRepository = winningNumberRepository;
        this.savedNumberRepository = savedNumberRepository;
        this.codec = codec;
        this.emailService = emailService;
    }

    // 토요일 22:00 KST — 당첨번호 수집 완료(21:30) 이후 발송
    @Scheduled(cron = "0 0 22 * * SAT", zone = "Asia/Seoul")
    @SchedulerLock(name = "send-draw-notifications", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional(readOnly = true)
    public void sendDrawResultNotifications() {
        List<EmailSubscription> subscribers = subscriptionRepository.findByVerifiedTrue();
        if (subscribers.isEmpty()) {
            log.info("당첨 결과 알림: 구독자 없음, 발송 생략");
            return;
        }

        WinningNumber latest = winningNumberRepository.findTopByOrderByRoundDesc().orElse(null);
        if (latest == null) {
            log.warn("당첨 결과 알림: 최신 회차 데이터 없음, 발송 생략");
            return;
        }

        List<Integer> winningNumbers = List.of(
                latest.getN1(), latest.getN2(), latest.getN3(),
                latest.getN4(), latest.getN5(), latest.getN6()
        );

        log.info("당첨 결과 알림 발송 시작: round={} subscribers={}", latest.getRound(), subscribers.size());
        int sent = 0;
        for (EmailSubscription sub : subscribers) {
            try {
                DrawNotificationContext ctx = buildContext(sub, latest, winningNumbers);
                emailService.sendDrawNotificationEmail(sub, ctx);
                sent++;
            } catch (Exception e) {
                log.warn("알림 발송 실패: email={} error={}", sub.getEmail(), e.getMessage());
            }
        }
        log.info("당첨 결과 알림 발송 완료: round={} sent={}/{}", latest.getRound(), sent, subscribers.size());
    }

    private DrawNotificationContext buildContext(EmailSubscription sub, WinningNumber latest,
                                                 List<Integer> winningNumbers) {
        List<DrawNotificationContext.SavedEntryResult> entries =
                savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(sub.getDeviceTokenHash())
                        .stream()
                        .map(saved -> {
                            List<Integer> savedNums = codec.fromStorageValue(saved.getNumbers());
                            int matchCount = (int) savedNums.stream().filter(winningNumbers::contains).count();
                            boolean bonusMatch = savedNums.contains(latest.getBonusNumber());
                            String rank = determineRank(matchCount, bonusMatch);
                            return new DrawNotificationContext.SavedEntryResult(
                                    savedNums, saved.getLabel(), matchCount, bonusMatch, rank);
                        })
                        .toList();

        return new DrawNotificationContext(
                latest.getRound(), latest.getDrawDate(),
                winningNumbers, latest.getBonusNumber(), entries
        );
    }

    private String determineRank(int matchCount, boolean bonusMatch) {
        return switch (matchCount) {
            case 6 -> "1등";
            case 5 -> bonusMatch ? "2등" : "3등";
            case 4 -> "4등";
            case 3 -> "5등";
            default -> "낙첨";
        };
    }
}
