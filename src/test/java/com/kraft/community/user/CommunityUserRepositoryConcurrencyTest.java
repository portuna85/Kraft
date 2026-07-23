package com.kraft.community.user;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * (provider, provider_id) UNIQUE 제약이 동시 가입 경합에서도 정확히 한 행만 허용하는지
 * 실제 MariaDB로 검증한다. OAuth 서비스는 2단계에서 이 제약 위반을 잡아 기존 사용자로
 * 재조회하는 로직을 구현하며, 이 테스트는 그 전제가 되는 DB 제약 자체를 고정한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("커뮤니티 사용자 동시 가입 경합 테스트 (실 MariaDB)")
class CommunityUserRepositoryConcurrencyTest {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.7")
            .withDatabaseName("kraft_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "false");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private CommunityUserRepository communityUserRepository;

    private final Clock clock = Clock.systemUTC();

    @Test
    @DisplayName("같은 (provider, providerId)로 동시에 가입해도 정확히 한 행만 만들어진다")
    void concurrentSignupWithSameProviderId_createsExactlyOneRow() throws Exception {
        String provider = "google";
        String providerId = "concurrent-signup-" + System.nanoTime();
        int threadCount = 4;

        List<Object> outcomes = runConcurrentlyAllowingFailure(threadCount, i ->
                communityUserRepository.save(new CommunityUser(
                        provider, providerId, "nickname-" + i, null, OffsetDateTime.now(clock))));

        long succeeded = outcomes.stream().filter(o -> o instanceof CommunityUser).count();
        long rejected = outcomes.stream().filter(o -> o instanceof DataIntegrityViolationException).count();

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(threadCount - 1);
        assertThat(communityUserRepository.findByProviderAndProviderId(provider, providerId)).isPresent();
    }

    private List<Object> runConcurrentlyAllowingFailure(int threadCount, IntFunction<Object> task)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            return task.apply(i);
                        } catch (Exception e) {
                            return e;
                        }
                    }))
                    .toList();
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            assertThat(allReady).as("모든 스레드가 준비될 때까지 기다림").isTrue();
            start.countDown();

            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }
}
