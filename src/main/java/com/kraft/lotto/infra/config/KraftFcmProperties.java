package com.kraft.lotto.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.fcm")
public class KraftFcmProperties {

    private boolean enabled = false;
    private String credentialsPath = "";
    private String drawResultTopic = "draw-result";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getDrawResultTopic() {
        return drawResultTopic;
    }

    public void setDrawResultTopic(String drawResultTopic) {
        this.drawResultTopic = drawResultTopic;
    }
}
