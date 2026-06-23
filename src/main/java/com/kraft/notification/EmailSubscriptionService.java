package com.kraft.notification;

import com.kraft.common.error.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Transactional
public class EmailSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(EmailSubscriptionService.class);

    private final EmailSubscriptionRepository repository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Clock clock;

    @Value("${kraft.public-base-url}")
    private String publicBaseUrl;

    @Value("${kraft.mail.from:noreply@kraft.io.kr}")
    private String mailFrom;

    public EmailSubscriptionService(EmailSubscriptionRepository repository,
                                    JavaMailSender mailSender,
                                    TemplateEngine templateEngine,
                                    Clock clock) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.clock = clock;
    }

    public void subscribe(String deviceTokenHash, String email) {
        Optional<EmailSubscription> existing = repository.findByDeviceTokenHash(deviceTokenHash);

        String verificationToken = UUID.randomUUID().toString();

        if (existing.isPresent()) {
            EmailSubscription sub = existing.get();
            if (sub.isVerified() && sub.getEmail().equalsIgnoreCase(email)) {
                // 이미 인증된 동일 이메일 — 재전송 없이 정상 처리
                return;
            }
            sub.updateEmail(email.toLowerCase(), verificationToken);
        } else {
            String unsubscribeToken = UUID.randomUUID().toString();
            repository.save(new EmailSubscription(
                    deviceTokenHash,
                    email.toLowerCase(),
                    verificationToken,
                    unsubscribeToken,
                    OffsetDateTime.now(clock)
            ));
        }

        sendVerificationEmail(email, verificationToken);
    }

    @Transactional(readOnly = true)
    public Optional<EmailSubscriptionStatusResponse> getStatus(String deviceTokenHash) {
        return repository.findByDeviceTokenHash(deviceTokenHash)
                .map(sub -> new EmailSubscriptionStatusResponse(sub.getEmail(), sub.isVerified()));
    }

    public String verify(String token) {
        EmailSubscription sub = repository.findByVerificationToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TOKEN", "유효하지 않은 인증 링크입니다."));

        if (!sub.isVerified()) {
            sub.verify(OffsetDateTime.now(clock));
            log.info("이메일 인증 완료: email={}", sub.getEmail());
        }
        return publicBaseUrl + "/saved?emailVerified=true";
    }

    public void unsubscribeByDevice(String deviceTokenHash) {
        repository.findByDeviceTokenHash(deviceTokenHash).ifPresent(sub -> {
            log.info("이메일 구독 해지: email={}", sub.getEmail());
            repository.delete(sub);
        });
    }

    public String unsubscribeByToken(String token) {
        EmailSubscription sub = repository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TOKEN", "유효하지 않은 수신 거부 링크입니다."));
        log.info("이메일 수신 거부: email={}", sub.getEmail());
        repository.delete(sub);
        return publicBaseUrl + "/saved?emailUnsubscribed=true";
    }

    private void sendVerificationEmail(String to, String verificationToken) {
        String verifyUrl = publicBaseUrl + "/api/v1/notifications/email/verify?token=" + verificationToken;

        Context ctx = new Context();
        ctx.setVariable("verifyUrl", verifyUrl);
        ctx.setVariable("siteUrl", publicBaseUrl);
        String html = templateEngine.process("email/verify", ctx);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("[KRAFT Lotto] 이메일 인증을 완료해 주세요");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("인증 이메일 발송: to={}", to);
        } catch (MailException | MessagingException e) {
            log.warn("인증 이메일 발송 실패: to={} error={}", to, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_SEND_FAILED",
                    "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public void sendDrawNotificationEmail(EmailSubscription sub, DrawNotificationContext ctx) {
        Context thymeleafCtx = new Context();
        thymeleafCtx.setVariable("round", ctx.round());
        thymeleafCtx.setVariable("drawDate", ctx.drawDate());
        thymeleafCtx.setVariable("winningNumbers", ctx.winningNumbers());
        thymeleafCtx.setVariable("bonusNumber", ctx.bonusNumber());
        thymeleafCtx.setVariable("savedEntries", ctx.savedEntries());
        thymeleafCtx.setVariable("siteUrl", publicBaseUrl);
        thymeleafCtx.setVariable("unsubscribeUrl",
                publicBaseUrl + "/api/v1/notifications/email/unsubscribe?token=" + sub.getUnsubscribeToken());
        String html = templateEngine.process("email/draw-result", thymeleafCtx);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(sub.getEmail());
            helper.setSubject(String.format("[KRAFT Lotto] 제%d회 당첨 결과 알림", ctx.round()));
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.warn("추첨 결과 이메일 발송 실패: to={} round={} error={}", sub.getEmail(), ctx.round(), e.getMessage());
        }
    }
}
