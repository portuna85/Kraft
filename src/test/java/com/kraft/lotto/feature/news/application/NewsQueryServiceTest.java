package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 조회 서비스")
class NewsQueryServiceTest {

    @Mock
    NewsArticleRepository repository;

    @Mock
    NewsSourceClassifier classifier;

    @InjectMocks
    NewsQueryService service;

    @Test
    @DisplayName("뉴스 목록을 페이지 단위로 반환한다")
    void returnsPagedArticles() {
        NewsArticleEntity entity = new NewsArticleEntity(
                "로또 뉴스", "https://example.com/1", "abc123",
                "설명", "뉴스원", LocalDateTime.now(), LocalDateTime.now());
        when(repository.findAllByOrderByPubDateDescCollectedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        NewsQueryService.NewsPage result = service.list(0, 20);

        assertThat(result.articles()).hasSize(1);
        assertThat(result.articles().get(0).title()).isEqualTo("로또 뉴스");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("결과가 없으면 빈 목록을 반환한다")
    void emptyRepositoryReturnsEmptyList() {
        when(repository.findAllByOrderByPubDateDescCollectedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        NewsQueryService.NewsPage result = service.list(0, 20);

        assertThat(result.articles()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("page가 음수면 예외가 발생한다")
    void listPageNegativeShouldThrow() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.list(-1, 20))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    @DisplayName("size가 0이면 예외가 발생한다")
    void listSizeZeroShouldThrow() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.list(0, 0))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }
}
