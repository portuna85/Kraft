package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("ApiCircuitBreakerRegistry")
class ApiCircuitBreakerRegistryTest {

    private static ApiCircuitBreakerRegistry registry() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = new ObjectProvider<>() {
            @Override
            public io.micrometer.core.instrument.MeterRegistry getObject() { return meterRegistry; }

            @Override
            public io.micrometer.core.instrument.MeterRegistry getIfAvailable(
                    java.util.function.Supplier<io.micrometer.core.instrument.MeterRegistry> defaultSupplier) {
                return meterRegistry;
            }
        };
        return new ApiCircuitBreakerRegistry(provider);
    }

    @Test
    @DisplayName("등록된 breaker를 snapshots에서 조회할 수 있다")
    void registeredBreakerAppearsInSnapshots() {
        ApiCircuitBreakerRegistry reg = registry();
        ApiCircuitBreaker breaker = ApiCircuitBreaker.disabled();

        reg.register("dhlottery", breaker);

        assertThat(reg.snapshots()).containsKey("dhlottery");
        assertThat(reg.snapshots().get("dhlottery").state()).isEqualTo("closed");
    }

    @Test
    @DisplayName("null client로 등록하면 breaker를 그대로 반환하고 snapshots에 추가되지 않는다")
    void nullClientIsIgnored() {
        ApiCircuitBreakerRegistry reg = registry();
        ApiCircuitBreaker breaker = ApiCircuitBreaker.disabled();

        ApiCircuitBreaker returned = reg.register(null, breaker);

        assertThat(returned).isSameAs(breaker);
        assertThat(reg.snapshots()).isEmpty();
    }

    @Test
    @DisplayName("빈 client로 등록하면 snapshots에 추가되지 않는다")
    void blankClientIsIgnored() {
        ApiCircuitBreakerRegistry reg = registry();
        reg.register("  ", ApiCircuitBreaker.disabled());

        assertThat(reg.snapshots()).isEmpty();
    }

    @Test
    @DisplayName("null breaker로 등록하면 null을 반환하고 snapshots에 추가되지 않는다")
    void nullBreakerIsIgnored() {
        ApiCircuitBreakerRegistry reg = registry();
        ApiCircuitBreaker returned = reg.register("client", null);

        assertThat(returned).isNull();
        assertThat(reg.snapshots()).isEmpty();
    }

    @Test
    @DisplayName("같은 client를 두 번 등록해도 gauge가 중복 등록되지 않는다")
    void duplicateRegistrationDoesNotDuplicateGauge() {
        ApiCircuitBreakerRegistry reg = registry();
        ApiCircuitBreaker b1 = new ApiCircuitBreaker(true, 2, 1000, 1);
        ApiCircuitBreaker b2 = new ApiCircuitBreaker(true, 3, 2000, 2);

        reg.register("smok", b1);
        reg.register("smok", b2);

        assertThat(reg.snapshots()).containsKey("smok");
    }

    @Test
    @DisplayName("상태 전이 리스너가 등록되어 breaker 상태가 바뀔 때 호출된다")
    void stateTransitionListenerIsWired() {
        ApiCircuitBreakerRegistry reg = registry();
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, 100, 1, now::get);

        reg.register("test", breaker);
        breaker.recordFailure();

        assertThat(breaker.stateName()).isEqualTo("open");
    }

    @Test
    @DisplayName("startup 시 meter registry가 늦게 준비되어도 gauge를 나중에 바인딩한다")
    void bindsGaugeAfterSingletonInitializationWhenRegistryArrivesLate() {
        AtomicReference<MeterRegistry> registryRef = new AtomicReference<>();
        ObjectProvider<MeterRegistry> provider = new ObjectProvider<>() {
            @Override
            public MeterRegistry getObject(Object... args) {
                return registryRef.get();
            }

            @Override
            public MeterRegistry getIfAvailable() {
                return registryRef.get();
            }
        };
        ApiCircuitBreakerRegistry reg = new ApiCircuitBreakerRegistry(provider);
        ApiCircuitBreaker breaker = ApiCircuitBreaker.disabled();

        reg.register("dhlottery", breaker);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        registryRef.set(meterRegistry);
        reg.afterSingletonsInstantiated();

        assertThat(meterRegistry.find("kraft.api.circuit_breaker.state")
                .tag("client", "dhlottery")
                .gauge()).isNotNull();
    }
}
