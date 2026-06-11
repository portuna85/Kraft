package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class InfoPageController {

    private static final DateTimeFormatter BUILD_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final WinningNumberRepository winningNumberRepository;
    private final Clock clock;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @GetMapping("/data-source")
    public String dataSource(Model model) {
        winningNumberRepository.findTopByOrderByRoundDesc().ifPresent(latest -> {
            model.addAttribute("latestStoredRound", latest.getRound());
            model.addAttribute("latestStoredDate",
                    latest.getDrawDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        });
        model.addAttribute("expectedRound",
                LottoDrawSchedule.expectedRound(LocalDate.now(clock)));
        model.addAttribute("changeLog",
                fetchLogQueryService.recentCollectionLogs(20));

        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp != null) {
            model.addAttribute("appVersion", bp.getVersion());
            if (bp.getTime() != null) {
                model.addAttribute("buildTimeText",
                        bp.getTime().atZone(ZoneId.of("Asia/Seoul")).format(BUILD_TIME_FMT));
            }
        }
        return "info/data-source";
    }
}
