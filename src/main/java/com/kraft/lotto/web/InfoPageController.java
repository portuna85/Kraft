package com.kraft.lotto.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InfoPageController {

    /** 구 URL 하위 호환 리다이렉트: /data-source → /info/data-source */
    @GetMapping("/data-source")
    public String dataSourceRedirect() {
        return "redirect:/info/data-source";
    }
}
