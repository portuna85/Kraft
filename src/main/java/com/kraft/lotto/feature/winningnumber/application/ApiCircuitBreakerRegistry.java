package com.kraft.lotto.feature.winningnumber.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ApiCircuitBreakerRegistry {

    private static final Logger log = LoggerFactory.getLogger(ApiCircuitBreakerRegistry.class);

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ConcurrentMap<String, ApiCircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> gaugeRegistered = new ConcurrentHashMap<>();

    public ApiCircuitBreakerRegistry(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public ApiCircuitBreaker register(String client, ApiCircuitBreaker breaker) {
        if (client == null || client.isBlank() || breaker == null) {
            return breaker;
        }
        String key = client.trim().toLowerCase();
        breakers.put(key, breaker);
        breaker.setStateTransitionListener((before, after) ->
                log.info("api circuit breaker state changed: client={}, {} -> {}", key, before, after));
        registerGaugeIfNeeded(key, breaker);
        return breaker;
    }

    public Map<String, Snapshot> snapshots() {
        Map<String, Snapshot> result = new TreeMap<>();
        breakers.forEach((client, breaker) -> result.put(client, new Snapshot(breaker.enabled(), breaker.stateName())));
        return result;
    }

    private void registerGaugeIfNeeded(String client, ApiCircuitBreaker breaker) {
        if (gaugeRegistered.putIfAbsent(client, Boolean.TRUE) != null) {
            return;
        }
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("kraft.api.circuit_breaker.state", breaker, ApiCircuitBreaker::stateCode)
                .description("Circuit breaker state by client (closed=0, half_open=1, open=2)")
                .tag("client", client)
                .register(meterRegistry);
    }

    public record Snapshot(boolean enabled, String state) {
    }
}

