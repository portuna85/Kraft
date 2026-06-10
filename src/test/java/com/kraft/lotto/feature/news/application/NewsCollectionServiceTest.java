package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.domain.NewsArticle;
import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 수집 서비스")
class NewsCollectionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-02T12:34:56Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    NewsArticleRepository repository;

    @Mock
    NewsRssClient rssClient;

    @Mock
    NewsArticlePersister persister;

    @Mock
    NewsBlockedDomainRepository blockedDomainRepository;

    @Mock
    NewsBlockedKeywordRepository blockedKeywordRepository;

    @Test
    @DisplayName("새 기사는 저장된다")
    void newArticlesAreSaved() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(false);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        verify(persister, times(1)).saveArticle(any());
    }

    @Test
    @DisplayName("이미 존재하는 링크는 건너뛴다")
    void duplicateLinkIsSkipped() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(true);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(persister, never()).saveArticle(any());
    }

    @Test
    @DisplayName("알에스에스가 비어 있으면 저장하지 않는다")
    void emptyRssSkipsAll() {
        when(rssClient.fetch()).thenReturn(List.of());

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        verify(persister, never()).saveArticle(any());
    }

    @Test
    @DisplayName("여러 기사 중 신규만 저장된다")
    void onlyNewArticlesAreSaved() {
        List<NewsArticle> fetched = List.of(
                article("https://example.com/new"),
                article("https://example.com/existing")
        );
        when(rssClient.fetch()).thenReturn(fetched);
        when(repository.existsByLinkHash(anyString()))
                .thenReturn(false)
                .thenReturn(true);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 기사 삭제는 보존 기간 이전 데이터를 삭제한다")
    void purgeDeletesOldArticles() {
        when(repository.deleteByCollectedAtBefore(any(LocalDateTime.class))).thenReturn(5);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        int deleted = service.purgeOldArticles();

        assertThat(deleted).isEqualTo(5);
        verify(repository).deleteByCollectedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("중복 기사가 저장 중 충돌해도 나머지 기사는 롤백되지 않는다")
    void collectDuplicateArticleShouldSkipWithoutRollback() {
        List<NewsArticle> fetched = List.of(
                article("https://example.com/1"),
                article("https://example.com/2")
        );
        when(rssClient.fetch()).thenReturn(fetched);
        when(repository.existsByLinkHash(anyString())).thenReturn(false);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
                .when(persister)
                .saveArticle(argThat(entity -> "https://example.com/1".equals(((NewsArticleEntity) entity).getLink())));

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(persister, times(2)).saveArticle(any());
    }

    @Test
    @DisplayName("수집 시 주입된 시계의 시각을 수집 시각에 사용한다")
    void collectUsesInjectedClock() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(false);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        service.collect();

        verify(persister).saveArticle(argThat(entity ->
                LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone())
                        .equals(((NewsArticleEntity) entity).getCollectedAt())));
    }

    @Test
    @DisplayName("수집 시 뉴스 출처 등급을 저장한다")
    void collectStoresSourceTier() {
        NewsArticle official = new NewsArticle(null, "테스트 제목", "https://example.com/official",
                "설명", "동행복권", LocalDateTime.now(), null);
        when(rssClient.fetch()).thenReturn(List.of(official));
        when(repository.existsByLinkHash(anyString())).thenReturn(false);

        NewsSourceClassifier classifier = new NewsSourceClassifier(List.of("동행복권"), List.of());
        NewsCollectionService service = new NewsCollectionService(
                repository,
                rssClient,
                persister,
                classifier,
                FIXED_CLOCK,
                30,
                List.of()
        );
        service.collect();

        verify(persister).saveArticle(argThat(entity ->
                ((NewsArticleEntity) entity).getSourceTier() == NewsSourceTier.OFFICIAL));
    }

    @Test
    @DisplayName("데이터베이스에 등록된 차단 도메인의 기사는 건너뛴다")
    void dbBlockedDomainIsSkipped() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://blocked.example.com/news")));
        when(blockedDomainRepository.findAllDomains()).thenReturn(List.of("blocked.example.com"));

        NewsCollectionService service = new NewsCollectionService(
                repository, rssClient, persister,
                new NewsSourceClassifier(List.of(), List.of()),
                FIXED_CLOCK, 30, List.of(), blockedDomainRepository);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(persister, never()).saveArticle(any());
    }

    @Test
    @DisplayName("데이터베이스에 등록된 차단 키워드가 설명에 포함된 기사는 건너뛴다")
    void dbBlockedKeywordInDescriptionIsSkipped() {
        NewsArticle article = new NewsArticle(null, "로또 소식", "https://example.com/news",
                "분양 로또 관련 설명", "출처", LocalDateTime.now(), null);
        when(rssClient.fetch()).thenReturn(List.of(article));
        when(blockedKeywordRepository.findAllKeywords()).thenReturn(List.of("분양 로또"));

        NewsCollectionService service = new NewsCollectionService(
                repository, rssClient, persister,
                new NewsSourceClassifier(List.of(), List.of()),
                FIXED_CLOCK, 30, List.of(), List.of(), null, blockedKeywordRepository);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(repository, never()).existsByLinkHash(anyString());
        verify(persister, never()).saveArticle(any());
    }

    @Test
    @DisplayName("설정 차단 키워드는 제목 외 링크와 출처에도 적용된다")
    void configuredBlockedKeywordAppliesToLinkAndSource() {
        NewsArticle article = new NewsArticle(null, "로또 소식", "https://example.com/powerball/news",
                "설명", "Powerball Daily", LocalDateTime.now(), null);
        when(rssClient.fetch()).thenReturn(List.of(article));

        NewsCollectionService service = new NewsCollectionService(
                repository, rssClient, persister,
                new NewsSourceClassifier(List.of(), List.of()),
                FIXED_CLOCK, 30, List.of(), List.of("powerball"), null, null);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(persister, never()).saveArticle(any());
    }

    private static NewsArticle article(String link) {
        return new NewsArticle(null, "테스트 제목", link, "설명", "출처",
                LocalDateTime.now(), null);
    }
}
