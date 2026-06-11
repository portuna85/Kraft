package com.kraft.lotto.web;

import com.kraft.lotto.infra.config.KraftAdProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdModelAdvice {

    private final KraftAdProperties adProperties;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
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
