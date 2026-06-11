package com.kraft.lotto.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.ad")
public class KraftAdProperties {

    private boolean enabled = false;

    /** Google AdSense Publisher ID (e.g. ca-pub-XXXXXXXXXXXXXXXXX) */
    private String adsenseClientId = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdsenseClientId() {
        return adsenseClientId;
    }

    public void setAdsenseClientId(String adsenseClientId) {
        this.adsenseClientId = adsenseClientId;
    }
}
