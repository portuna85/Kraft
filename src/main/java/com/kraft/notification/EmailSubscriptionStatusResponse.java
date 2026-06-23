package com.kraft.notification;

public record EmailSubscriptionStatusResponse(
        String email,
        boolean verified
) {
}
