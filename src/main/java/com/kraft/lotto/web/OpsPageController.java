package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class OpsPageController {

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final KraftCollectProperties collectProperties;
    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @GetMapping("/admin/ops")
    public String opsDashboard(
            @RequestParam(defaultValue = "200") @Min(1) @Max(2000) int reasonLimit,
            @RequestParam(defaultValue = "100") @Min(1) @Max(2000) int logLimit,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(5) @Max(100) int pageSize,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            Model model
    ) {
        int safeReasonLimit = OpsQueryParams.normalizeReasonLimit(reasonLimit);
        int safeLogLimit = OpsQueryParams.normalizeLogLimit(logLimit);
        int safePage = PublicQueryParams.normalizePage(page);
        int safePageSize = PublicQueryParams.normalizeSize(pageSize);
        String safeReason = OpsQueryParams.normalizeReason(reason);
        OpsQueryParams.Range range = OpsQueryParams.normalizeRange(drwNoFrom, drwNoTo);

        FetchFailureOverviewDto overview = fetchLogQueryService.failureOverview(
                safeReasonLimit,
                safeLogLimit,
                safeReason,
                range.from(),
                range.to()
        );
        var pagedFailures = fetchLogQueryService.listRecentFailuresPage(
                safePage,
                safePageSize,
                safeReason,
                range.from(),
                range.to()
        );

        model.addAttribute("overview", overview);
        model.addAttribute("pagedFailures", pagedFailures.rows());
        model.addAttribute("reason", safeReason == null ? "" : safeReason);
        model.addAttribute("drwNoFrom", range.from());
        model.addAttribute("drwNoTo", range.to());
        model.addAttribute("reasonLimit", safeReasonLimit);
        model.addAttribute("logLimit", safeLogLimit);
        model.addAttribute("page", pagedFailures.page());
        model.addAttribute("pageSize", pagedFailures.pageSize());
        model.addAttribute("hasNext", pagedFailures.hasNext());
        model.addAttribute("retentionStatus", OpsRetentionStatusSupport.resolve(fetchLogQueryService, collectProperties));

        addStatusAttributes(model);
        return "admin-ops";
    }

    private void addStatusAttributes(Model model) {
        int latestStored = winningNumberRepository.findMaxRound().orElse(0);
        int expected = LottoDrawSchedule.expectedRound(LocalDate.now(clock));
        model.addAttribute("latestStoredRound", latestStored);
        model.addAttribute("expectedRound", expected);
        model.addAttribute("roundGap", expected - latestStored);
        model.addAttribute("activeProfiles",
                Optional.of(environment.getActiveProfiles())
                        .filter(p -> p.length > 0)
                        .map(Arrays::asList)
                        .orElse(java.util.List.of("default")));
        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp != null) {
            model.addAttribute("buildVersion", bp.getVersion());
            model.addAttribute("buildTime", bp.getTime());
        }
    }
}
