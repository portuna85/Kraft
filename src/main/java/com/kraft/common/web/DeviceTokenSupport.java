package com.kraft.common.web;

import com.kraft.common.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenSupport {

    public String headerName() {
        return "X-Device-Token";
    }

    public String requireHashedToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEVICE_TOKEN_REQUIRED", "X-Device-Token 헤더가 필요합니다.");
        }
        return sha256Hex(token.trim());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) {
                builder.append(String.format("%02x", valueByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }
}
