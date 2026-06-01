package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.news.domain.NewsArticle;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 수집 서비스")
class NewsCollectionServiceTest {

    @Mock
    NewsArticleRepository repository;

    @Mock
    NewsRssClient rssClient;

    @Test
    @DisplayName("새 기사는 저장된다")
    void newArticlesAreSaved() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(false);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 링크는 건너뛴다")
    void duplicateLinkIsSkipped() {
        when(rssClient.fetch()).thenReturn(List.of(article("https://example.com/1")));
        when(repository.existsByLinkHash(anyString())).thenReturn(true);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("RSS가 비어 있으면 저장하지 않는다")
    void emptyRssSkipsAll() {
        when(rssClient.fetch()).thenReturn(List.of());

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(0);
        verify(repository, never()).save(any());
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

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, 30);
        NewsCollectionService.NewsCollectResult result = service.collect();

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("purgeOldArticles는 보존 기간 이전 데이터를 삭제한다")
    void purgeDeletesOldArticles() {
        when(repository.deleteByCollectedAtBefore(any(LocalDateTime.class))).thenReturn(5);

        NewsCollectionService service = new NewsCollectionService(repository, rssClient, 30);
        int deleted = service.purgeOldArticles();

        assertThat(deleted).isEqualTo(5);
        verify(repository).deleteByCollectedAtBefore(any(LocalDateTime.class));
    }

    private static NewsArticle article(String link) {
        return new NewsArticle(null, "테스트 제목", link, "설명", "출처",
                LocalDateTime.now(), null);
    }
}
