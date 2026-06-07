package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import com.kraft.lotto.support.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpsApi
@RestController
@RequestMapping("/ops/collect")
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
public class OpsCollectionController {

    private final LottoCollectionCommandService collectionCommandService;
    private final OpsCollectionFacade opsCollectionFacade;
    private final WinningStoreCollector winningStoreCollector;
    private final KraftSecurityProperties securityProperties;

    @Autowired
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "KraftSecurityProperties is a Spring-managed singleton; storing it is safe."
    )
    public OpsCollectionController(LottoCollectionCommandService collectionCommandService,
                                   OpsCollectionFacade opsCollectionFacade,
                                   WinningStoreCollector winningStoreCollector,
                                   KraftSecurityProperties securityProperties) {
        this.collectionCommandService = collectionCommandService;
        this.opsCollectionFacade = opsCollectionFacade;
        this.winningStoreCollector = winningStoreCollector;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/status")
    @Operation(summary = "Get current collection job status")
    public CollectStatusResponse collectStatus() {
        return collectionCommandService.getStatus();
    }

    @PostMapping
    @Operation(summary = "Collect winning numbers up to latest round")
    public CollectResponse collectLatest(HttpServletRequest request) {
        return opsCollectionFacade.collectLatest(
                MDC.get("requestId"),
                ClientIpResolver.resolve(request, securityProperties.getTrustedProxies()));
    }

    @PostMapping("/missing")
    @Operation(summary = "Collect only missing rounds once")
    public CollectResponse collectMissing(HttpServletRequest request) {
        return opsCollectionFacade.collectMissing(
                MDC.get("requestId"),
                ClientIpResolver.resolve(request, securityProperties.getTrustedProxies()));
    }

    @PostMapping("/stores")
    @Operation(summary = "Collect winning stores (1st/2nd prize) for a specific round")
    public Map<String, Object> collectStores(@RequestParam int round) {
        boolean saved = winningStoreCollector.collectStores(round);
        return Map.of("round", round, "saved", saved);
    }

    @PostMapping("/stores/backfill-regions")
    @Operation(summary = "Backfill sido/sigungu from existing address for stores with null region")
    public Map<String, Object> backfillRegions() {
        int updated = winningStoreCollector.backfillRegions();
        return Map.of("updated", updated);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Re-fetch a specific round from API even if already collected (refresh=true)")
    public CollectResponse refreshRound(@RequestParam int round) {
        return collectionCommandService.collectOneRefresh(round);
    }
}
