package com.kraft.lotto.feature.winningnumber.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class ApiCircuitBreakerRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ApiCircuitBreakerRegistry.class);

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ConcurrentMap<String, ApiCircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> gaugeBound = new ConcurrentHashMap<>();

    public ApiCircuitBreakerRegistry(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public ApiCircuitBreaker register(String client, ApiCircuitBreaker breaker) {
        if (client == null || client.isBlank() || breaker == null) {
            return breaker;
        }
        String key = client.trim().toLowerCase();
        breakers.put(key, breaker);
        breaker.setStateTransitionListener((before, after) -> {
            log.info("api circuit breaker state changed: client={}, {} -> {}", key, before, after);
            MeterRegistry meterRegistry = activeMeterRegistry();
            if (meterRegistry == null) {
                log.debug("meter registry unavailable for circuit breaker transition metric: client={}", key);
                return;
            }
            Counter.builder("kraft.api.circuit_breaker.transitions")
                    .description("Circuit breaker state transitions")
                    .tag("client", key)
                    .tag("from", before)
                    .tag("to", after)
                    .register(meterRegistry)
                    .increment();
        });
        registerGaugeIfNeeded(key, breaker);
        return breaker;
    }

    public Map<String, Snapshot> snapshots() {
        Map<String, Snapshot> result = new TreeMap<>();
        breakers.forEach((client, breaker) -> result.put(client, new Snapshot(breaker.enabled(), breaker.stateName())));
        return result;
    }

    @Override
    public void afterSingletonsInstantiated() {
        breakers.forEach(this::bindGaugeIfPossible);
    }

    private void registerGaugeIfNeeded(String client, ApiCircuitBreaker breaker) {
        bindGaugeIfPossible(client, breaker);
    }

    private void bindGaugeIfPossible(String client, ApiCircuitBreaker breaker) {
        if (gaugeBound.containsKey(client)) {
            return;
        }
        MeterRegistry meterRegistry = activeMeterRegistry();
        if (meterRegistry == null) {
            log.debug("meter registry unavailable for circuit breaker gauge: client={}", client);
            return;
        }
        if (gaugeBound.putIfAbsent(client, Boolean.TRUE) != null) {
            return;
        }
        Gauge.builder("kraft.api.circuit_breaker.state", breaker, ApiCircuitBreaker::stateCode)
                .description("Circuit breaker state by client (closed=0, half_open=1, open=2)")
                .tag("client", client)
                .register(meterRegistry);
    }

    private MeterRegistry activeMeterRegistry() {
        return meterRegistryProvider.getIfAvailable();
    }

    public record Snapshot(boolean enabled, String state) {
    }
}
