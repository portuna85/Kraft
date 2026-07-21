package com.kraft.winningnumber;

import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rounds")
public class RoundsApiController {

    private final WinningNumberQueryService winningNumberQueryService;

    public RoundsApiController(WinningNumberQueryService winningNumberQueryService) {
        this.winningNumberQueryService = winningNumberQueryService;
    }

    // PublicApiCacheControlFilter가 이론상 이 응답에도 동일한 Cache-Control을 다시 설정하지만,
    // 실제 운영 환경에서 컨트롤러가 일반 객체를 반환하면 필터가 헤더를 못 붙이는 사례가
    // 확인되어(2026-07-21 배포 실패로 발견) ResponseEntity로 명시적으로 설정한다.
    @GetMapping("/latest")
    public ResponseEntity<WinningNumberResponse> latest() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(winningNumberQueryService.getLatest());
    }

    @GetMapping("/freshness")
    public ResponseEntity<RoundFreshnessResponse> freshness() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(winningNumberQueryService.getFreshness());
    }
}
