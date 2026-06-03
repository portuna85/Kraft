package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class InfoPageController {

    private static final int CHANGE_LOG_LIMIT = 20;

    private final LottoFetchLogQueryService fetchLogQueryService;

    @GetMapping("/methodology")
    public String methodology() {
        return "info/methodology";
    }

    @GetMapping("/data-source")
    public String dataSource(Model model) {
        model.addAttribute("changeLog", fetchLogQueryService.recentCollectionLogs(CHANGE_LOG_LIMIT));
        return "info/data-source";
    }

    @GetMapping("/faq")
    public String faq() {
        return "info/faq";
    }

    @GetMapping("/responsible-play")
    public String responsiblePlay() {
        return "info/responsible-play";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "info/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "info/terms";
    }

    @GetMapping("/contact")
    public String contact() {
        return "info/contact";
    }
}
