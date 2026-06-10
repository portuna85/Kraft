package com.kraft.lotto.web;

import com.kraft.lotto.feature.push.application.DeviceTokenService;
import com.kraft.lotto.feature.push.domain.Platform;
import com.kraft.lotto.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1/push")
@Validated
@RequiredArgsConstructor
@Tag(name = "Push", description = "푸시 알림 디바이스 토큰 관리")
public class PushApiController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/token")
    @Operation(summary = "디바이스 토큰 등록 또는 갱신")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody @Valid RegisterTokenRequest req) {
        deviceTokenService.register(req.token(), req.platform());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/token")
    @Operation(summary = "디바이스 토큰 해제")
    public ResponseEntity<ApiResponse<Void>> unregister(@RequestBody @Valid UnregisterTokenRequest req) {
        deviceTokenService.unregister(req.token());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record RegisterTokenRequest(
            @NotBlank @Size(max = 255) String token,
            @NotNull Platform platform
    ) {
    }

    public record UnregisterTokenRequest(
            @NotBlank @Size(max = 255) String token
    ) {
    }
}
