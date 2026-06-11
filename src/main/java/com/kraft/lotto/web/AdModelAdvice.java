package com.kraft.lotto.web;

import com.kraft.lotto.infra.config.KraftAdProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdModelAdvice {

    private final KraftAdProperties adProperties;

    public AdModelAdvice(KraftAdProperties adProperties) {
        this.adProperties = adProperties;
    }

    @ModelAttribute("adEnabled")
    public boolean adEnabled() {
        return adProperties.isEnabled() && !adProperties.getAdsenseClientId().isBlank();
    }

    @ModelAttribute("adsenseClientId")
    public String adsenseClientId() {
        return adProperties.getAdsenseClientId();
    }
}
