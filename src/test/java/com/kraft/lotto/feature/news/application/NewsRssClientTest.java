package com.kraft.lotto.feature.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.news.domain.NewsArticle;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 알에스에스 클라이언트")
class NewsRssClientTest {

    @Mock
    RestClient restClient;

    @Test
    @DisplayName("유효한 알에스에스 엑스엠엘을 파싱하면 뉴스 목록을 반환한다")
    void parsesValidRssXml() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse(sampleRss());

        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).title()).isEqualTo("로또 1등 당첨자 인터뷰");
        assertThat(articles.get(0).source()).isEqualTo("뉴스원");
        assertThat(articles.get(0).link()).isEqualTo("https://example.com/news/1");
    }

    @Test
    @DisplayName("최대 기사 수 제한이 적용된다")
    void maxArticlesLimitIsApplied() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 1, List.of());
        List<NewsArticle> articles = client.parse(sampleRss());

        assertThat(articles).hasSize(1);
    }

    @Test
    @DisplayName("링크가 없는 항목은 건너뛴다")
    void itemWithoutLinkIsSkipped() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse(rssWithoutLink());

        assertThat(articles).isEmpty();
    }

    @Test
    @DisplayName("잘못된 엑스엠엘이면 빈 목록을 반환한다")
    void malformedXmlReturnsEmptyList() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse("<invalid>xml<<</invalid>");

        assertThat(articles).isEmpty();
    }

    @Test
    @DisplayName("설명 에이치티엠엘 태그가 제거된다")
    void descriptionHtmlTagsAreStripped() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse(rssWithHtmlDescription());

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).description()).doesNotContain("<");
        assertThat(articles.get(0).description()).contains("로또");
    }

    @Test
    @DisplayName("발행일가 없어도 파싱된다")
    void itemWithoutPubDateIsParsed() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse(rssWithoutPubDate());

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).pubDate()).isNull();
    }

    @Test
    @DisplayName("가져오기 — 빈 응답이면 빈 목록을 반환한다")
    void emptyResponseReturnsEmptyList() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10, List.of());
        List<NewsArticle> articles = client.parse("");

        assertThat(articles).isEmpty();
    }

    @Test
    @DisplayName("제외 키워드가 포함된 제목은 필터링된다")
    void excludeKeywordFiltersArticle() {
        NewsRssClient client = new NewsRssClient(restClient, "https://example.com/rss", 10,
                List.of("아파트 로또", "분양 로또"));
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel>
                  <item>
                    <title>아파트 로또 청약 경쟁률</title>
                    <link>https://example.com/news/a</link>
                  </item>
                  <item>
                    <title>로또 1등 당첨번호 발표</title>
                    <link>https://example.com/news/b</link>
                  </item>
                </channel></rss>
                """;

        List<NewsArticle> articles = client.parse(rss);

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).title()).isEqualTo("로또 1등 당첨번호 발표");
    }

    private static String sampleRss() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>로또 1등 당첨자 인터뷰</title>
                      <link>https://example.com/news/1</link>
                      <description>이번 주 로또 1등 당첨자 이야기</description>
                      <pubDate>Mon, 01 Jun 2026 09:00:00 +0900</pubDate>
                      <source url="https://example.com">뉴스원</source>
                    </item>
                    <item>
                      <title>이번 주 로또 당첨번호 발표</title>
                      <link>https://example.com/news/2</link>
                      <description>당첨번호 안내</description>
                      <pubDate>Sun, 31 May 2026 22:30:00 +0900</pubDate>
                      <source url="https://example.com">로또뉴스</source>
                    </item>
                  </channel>
                </rss>
                """;
    }

    private static String rssWithoutLink() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>제목만 있음</title>
                    </item>
                  </channel>
                </rss>
                """;
    }

    private static String rssWithHtmlDescription() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>로또 뉴스</title>
                      <link>https://example.com/news/3</link>
                      <description><![CDATA[<p><b>로또</b> 당첨번호가 발표되었습니다.</p>]]></description>
                    </item>
                  </channel>
                </rss>
                """;
    }

    private static String rssWithoutPubDate() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>날짜 없는 뉴스</title>
                      <link>https://example.com/news/4</link>
                    </item>
                  </channel>
                </rss>
                """;
    }
}
