package com.kraft.lotto.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InfoPageController {

    @GetMapping("/methodology")
    public String methodology() {
        return "info/methodology";
    }

    @GetMapping("/data-source")
    public String dataSource() {
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
