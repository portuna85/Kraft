package com.kraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.operationlog.WinningNumberOperationLogRepository;
import com.kraft.recommend.LottoRecommendationService;
import com.kraft.saved.SavedNumberRepository;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MockExternalFetchClientConfig.class)
abstract class BaseApiIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected WinningNumberRepository winningNumberRepository;

    @Autowired
    protected SavedNumberRepository savedNumberRepository;

    @Autowired
    protected WinningNumberOperationLogRepository winningNumberOperationLogRepository;

    @Autowired
    protected LottoRecommendationService lottoRecommendationService;

    @Autowired
    private CacheManager cacheManager;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        winningNumberOperationLogRepository.deleteAll();
        savedNumberRepository.deleteAll();
        winningNumberRepository.deleteAll();
        winningNumberRepository.save(new WinningNumber(
                1200,
                LocalDate.of(2026, 6, 13),
                3, 11, 19, 28, 34, 42,
                7,
                2_000_000_000L,
                0L, 0, 0L, 0L,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        ));
        winningNumberRepository.save(new WinningNumber(
                1199,
                LocalDate.of(2026, 6, 6),
                1, 9, 17, 23, 31, 45,
                8,
                1_800_000_000L,
                0L, 0, 0L, 0L,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        ));
        // 리포지토리 직접 시딩은 WinningNumbersCollectedEvent를 발행하지 않으므로,
        // 추천 서비스의 이력 캐시를 수동으로 갱신해야 R2 fail-closed가 오탐하지 않는다.
        lottoRecommendationService.refreshHistoryCache();
    }

    protected long extractId(String response) throws Exception {
        return OBJECT_MAPPER.readTree(response).path("savedNumber").path("id").asLong();
    }
}
