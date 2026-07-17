package com.kraft.ops;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InfoController {

    private final Clock clock;

    public InfoController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "service", "kraft-lotto",
                "status", "정상",
                "timezone", ZoneId.of("Asia/Seoul").getId(),
                "checkedAt", Instant.now(clock).atZone(ZoneId.of("Asia/Seoul")).toString()
        );
    }
}
