package com.kraft.ops;

import com.kraft.operationlog.WinningNumberOperationLogPageResponse;
import com.kraft.winningnumber.WinningNumberResponse;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    @GetMapping("/summary")
    public OpsSummaryResponse summary(@RequestHeader(name = "X-Ops-Token", required = false) String token) {
        return opsService.getSummary(token);
    }

    @GetMapping("/logs")
    public WinningNumberOperationLogPageResponse logs(
            @RequestHeader(name = "X-Ops-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String executionStatus,
            @RequestParam(required = false) Integer round,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return opsService.getRecentOperationLogs(token, page, size, operationType, executionStatus, round, from, to);
    }

    @PostMapping("/rounds")
    public WinningNumberResponse upsertRound(@RequestHeader(name = "X-Ops-Token", required = false) String token,
                                             @Valid @RequestBody WinningNumberUpsertRequest request) {
        return opsService.upsertWinningNumber(token, request);
    }

    @PostMapping("/collect/latest")
    public WinningNumberResponse collectLatest(@RequestHeader(name = "X-Ops-Token", required = false) String token) {
        return opsService.collectLatestWinningNumber(token);
    }

    @PostMapping("/collect/{round}")
    public WinningNumberResponse collectRound(@RequestHeader(name = "X-Ops-Token", required = false) String token,
                                              @PathVariable int round) {
        return opsService.collectWinningNumber(token, round);
    }
}
