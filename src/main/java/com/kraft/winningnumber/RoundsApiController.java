package com.kraft.winningnumber;

import java.util.concurrent.TimeUnit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rounds")
public class RoundsApiController {

    private final WinningNumberQueryService winningNumberQueryService;

    public RoundsApiController(WinningNumberQueryService winningNumberQueryService) {
        this.winningNumberQueryService = winningNumberQueryService;
    }

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

    @GetMapping
    public WinningNumberListResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return winningNumberQueryService.list(page, size);
    }

    @GetMapping("/{round}")
    public WinningNumberResponse byRound(@PathVariable @Min(1) int round) {
        return winningNumberQueryService.getByRound(round);
    }
}
