package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberPageDto;
import com.kraft.lotto.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1/rounds")
@Validated
@RequiredArgsConstructor
@Tag(name = "Rounds", description = "당첨번호 조회")
public class RoundsApiController {

    private final WinningNumberQueryService queryService;

    @GetMapping("/latest")
    @Operation(summary = "최신 회차 당첨번호 조회")
    public ResponseEntity<ApiResponse<WinningNumberDto>> latest() {
        return ResponseEntity.ok(ApiResponse.success(queryService.getLatest()));
    }

    @GetMapping("/{round}")
    @Operation(summary = "특정 회차 당첨번호 조회")
    public ResponseEntity<ApiResponse<WinningNumberDto>> byRound(
            @PathVariable @Min(1) int round) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getByRound(round)));
    }

    @GetMapping
    @Operation(summary = "회차 목록 (페이지네이션)")
    public ResponseEntity<ApiResponse<WinningNumberPageDto>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(queryService.list(page, size)));
    }
}
