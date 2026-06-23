package com.kraft.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailSubscriptionRequest(
        @NotBlank @Email @Size(max = 254)
        String email
) {
}
