package com.kraft.lotto.infra.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.api.key")
@Validated
public class KraftApiKeyProperties {

    private boolean enabled = false;
    private List<String> keys = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getKeys() {
        return List.copyOf(keys);
    }

    public void setKeys(List<String> keys) {
        this.keys = keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }
}
