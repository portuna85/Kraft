package com.kraft.saved;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.common.lotto.LottoNumberCodec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * P1-06: 잠금 행의 created_at/last_used_at upsert 동작과 고아 행 정리 쿼리를 실제 MariaDB로
 * 검증한다(H2는 ON DUPLICATE KEY UPDATE·NOT EXISTS 서브쿼리 조합의 native SQL 방언이
 * 다를 수 있어 SavedNumbersConcurrencyTest와 같은 방식으로 실제 MariaDB를 띄운다).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("저장번호 클라이언트 잠금 행 수명 테스트 (실 MariaDB)")
class SavedNumberClientLockRepositoryTest {

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
    private SavedNumberClientLockRepository lockRepository;

    @Autowired
    private SavedNumberRepository savedNumberRepository;

    private final LottoNumberCodec lottoNumberCodec = new LottoNumberCodec();

    @BeforeEach
    void cleanUp() {
        savedNumberRepository.deleteAll();
        lockRepository.deleteAll();
    }

    @Test
    @DisplayName("최초 upsert는 created_at·last_used_at을 함께 세팅하고, 재호출은 last_used_at만 갱신한다")
    void ensureLockRowExists_upsertsCreatedAndLastUsedAt() {
        String token = "lock-upsert-token-" + System.nanoTime();
        OffsetDateTime firstNow = OffsetDateTime.parse("2026-01-01T00:00:00Z").withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime secondNow = firstNow.plusDays(5);

        lockRepository.ensureLockRowExists(token, firstNow);
        SavedNumberClientLock afterFirst = lockRepository.findById(token).orElseThrow();
        assertThat(afterFirst.getCreatedAt()).isEqualToIgnoringNanos(firstNow);
        assertThat(afterFirst.getLastUsedAt()).isEqualToIgnoringNanos(firstNow);

        lockRepository.ensureLockRowExists(token, secondNow);
        SavedNumberClientLock afterSecond = lockRepository.findById(token).orElseThrow();
        assertThat(afterSecond.getCreatedAt()).isEqualToIgnoringNanos(firstNow);
        assertThat(afterSecond.getLastUsedAt()).isEqualToIgnoringNanos(secondNow);
    }

    @Test
    @DisplayName("저장번호가 없고 오래된 잠금만 삭제하고, 저장번호가 있거나 최근인 잠금은 보존한다")
    void deleteOrphansOlderThan_removesOnlyOldOrphans() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T00:00:00Z").withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime cutoff = now.minusDays(30);
        OffsetDateTime old = cutoff.minusDays(1);
        OffsetDateTime recent = cutoff.plusDays(1);

        String orphanOld = "orphan-old-" + System.nanoTime();
        String orphanWithSaved = "orphan-with-saved-" + System.nanoTime();
        String orphanRecent = "orphan-recent-" + System.nanoTime();

        lockRepository.ensureLockRowExists(orphanOld, old);
        lockRepository.ensureLockRowExists(orphanWithSaved, old);
        lockRepository.ensureLockRowExists(orphanRecent, recent);

        savedNumberRepository.save(new SavedNumber(
                orphanWithSaved,
                lottoNumberCodec.toStorageValue(java.util.List.of(1, 2, 3, 4, 5, 6)),
                null, "MANUAL", now));

        int deleted = lockRepository.deleteOrphansOlderThan(cutoff);

        assertThat(deleted).isEqualTo(1);
        assertThat(lockRepository.findById(orphanOld)).isEmpty();
        assertThat(lockRepository.findById(orphanWithSaved)).isPresent();
        assertThat(lockRepository.findById(orphanRecent)).isPresent();
    }
}
