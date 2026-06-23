package com.kraft.notification;

import com.kraft.common.web.DeviceTokenSupport;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/email")
public class EmailNotificationController {

    private final EmailSubscriptionService service;
    private final DeviceTokenSupport deviceTokenSupport;

    public EmailNotificationController(EmailSubscriptionService service,
                                       DeviceTokenSupport deviceTokenSupport) {
        this.service = service;
        this.deviceTokenSupport = deviceTokenSupport;
    }

    @PostMapping
    public ResponseEntity<Void> subscribe(
            @RequestHeader("X-Device-Token") String deviceToken,
            @RequestBody @Valid EmailSubscriptionRequest request) {
        String tokenHash = deviceTokenSupport.requireHashedToken(deviceToken);
        service.subscribe(tokenHash, request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    public ResponseEntity<EmailSubscriptionStatusResponse> getStatus(
            @RequestHeader("X-Device-Token") String deviceToken) {
        String tokenHash = deviceTokenSupport.requireHashedToken(deviceToken);
        return service.getStatus(tokenHash)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(
            @RequestHeader("X-Device-Token") String deviceToken) {
        String tokenHash = deviceTokenSupport.requireHashedToken(deviceToken);
        service.unsubscribeByDevice(tokenHash);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam("token") String token) {
        String redirectUrl = service.verify(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribeByToken(@RequestParam("token") String token) {
        String redirectUrl = service.unsubscribeByToken(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
