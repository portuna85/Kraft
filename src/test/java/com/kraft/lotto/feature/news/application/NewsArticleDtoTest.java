package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.news.web.dto.NewsArticleDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("뉴스 기사 DTO")
class NewsArticleDtoTest {

    @Test
    @DisplayName("pubDate가 있으면 포맷된 문자열로 변환한다")
    void formatsPubDate() {
        LocalDateTime pubDate = LocalDateTime.of(2026, 6, 1, 12, 30);

        NewsArticleDto dto = NewsArticleDto.of(1L, "제목", "https://example.com",
                "설명", "출처", pubDate, null, null);

        assertThat(dto.pubDateFormatted()).isEqualTo("2026.06.01 12:30");
    }

    @Test
    @DisplayName("pubDate가 null이면 빈 문자열을 반환한다")
    void nullPubDateReturnsEmptyString() {
        NewsArticleDto dto = NewsArticleDto.of(1L, "제목", "https://example.com",
                "설명", "출처", null, null, null);

        assertThat(dto.pubDateFormatted()).isEmpty();
    }
}
