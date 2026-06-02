package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.news.domain.NewsArticle;
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
    @DisplayName("RSS가 비어 있으면 저장하지 않는다")
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
    @DisplayName("purgeOldArticles는 보존 기간 이전 데이터를 삭제한다")
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
    @DisplayName("수집 시 주입된 Clock의 시각을 collectedAt에 사용한다")
    void collectUsesInjectedClock() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(false);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, persister, FIXED_CLOCK, 30);
        service.collect();

        verify(persister).saveArticle(argThat(entity ->
                LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone())
                        .equals(((NewsArticleEntity) entity).getCollectedAt())));
    }

    private static NewsArticle article(String link) {
        return new NewsArticle(null, "테스트 제목", link, "설명", "출처",
                LocalDateTime.now(), null);
    }
}
