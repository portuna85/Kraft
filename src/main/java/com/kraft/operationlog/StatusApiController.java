package com.kraft.operationlog;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusApiController {

    private final WinningNumberOperationLogService operationLogService;

    public StatusApiController(WinningNumberOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<PublicIncidentResponse>> incidents() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(operationLogService.getPublicIncidents());
    }
}
