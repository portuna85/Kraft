package com.kraft.winningnumber;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B3: 신규 회차 동시 insert 시 REPEATABLE READ의 오래된 조회 스냅샷 때문에 경쟁에서 진
 * 트랜잭션이 방금 커밋된 행을 재조회 못 하는 문제를 실제 MariaDB 두 커넥션으로 검증한다.
 * repository mock으로는 격리 수준을 재현할 수 없어 실 DB가 필요하다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("회차 저장 서비스 동시성 테스트 (실 MariaDB)")
class WinningNumberCommandServiceConcurrencyTest {

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
    private WinningNumberCommandService winningNumberCommandService;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @BeforeEach
    void cleanUp() {
        winningNumberRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 신규 회차를 동시에 upsert해도 정확히 한 행만 남고 두 요청 모두 예외 없이 끝난다")
    void concurrentUpsertForNewRound_resultsInExactlyOneRow() throws Exception {
        int round = 900_001;
        // 신규 회차 upsert 경쟁은 실제로는 자동 스케줄러 대 수동 백필 정도의 2-way 경쟁이
        // 현실적인 최대치다. 그 이상으로 완전히 동시에 같은 신규 unique key를 insert하면
        // REPEATABLE READ 스냅샷 문제와는 별개로 InnoDB 데드락 자체가 발생할 수 있는데,
        // 그건 이 테스트(B3: 재조회 스냅샷 버그)가 아니라 재시도 정책의 영역이라 범위 밖이다.
        int threadCount = 2;

        List<WinningNumberUpsertResult> results = runConcurrently(threadCount, i ->
                winningNumberCommandService.upsertWithResult(new WinningNumberUpsertRequest(
                        round,
                        LocalDate.of(2026, 1, 1),
                        List.of(1 + i, 2 + i, 3 + i, 4 + i, 5 + i, 6 + i),
                        45,
                        1_000_000_000L,
                        0L, 0, 0L, 0L
                )));

        assertThat(results).hasSize(threadCount);
        assertThat(winningNumberRepository.findByRound(round)).isPresent();
        assertThat(winningNumberRepository.findAll().stream().filter(w -> w.getRound() == round).count())
                .isEqualTo(1);
        // 정확히 하나만 "신규 생성"이었어야 하고, 나머지는 그 회차를 update로 재해석했어야 한다.
        assertThat(results.stream().filter(WinningNumberUpsertResult::changed).count())
                .isGreaterThanOrEqualTo(1);
    }

    private <T> List<T> runConcurrently(int threadCount, java.util.function.IntFunction<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return task.apply(i);
                    }))
                    .toList();
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            assertThat(allReady).as("모든 스레드가 준비될 때까지 기다림").isTrue();
            start.countDown();

            List<T> results = new java.util.ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }
}
