package com.kraft.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSubscriptionRepository extends JpaRepository<EmailSubscription, Long> {

    Optional<EmailSubscription> findByDeviceTokenHash(String deviceTokenHash);

    Optional<EmailSubscription> findByVerificationToken(String verificationToken);

    Optional<EmailSubscription> findByUnsubscribeToken(String unsubscribeToken);

    List<EmailSubscription> findByVerifiedTrue();
}
