package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
@DisplayName("SEO 컨트롤러")
class SeoControllerTest {

    @Mock
    Environment environment;

    @InjectMocks
    SeoController controller;

    @Test
    @DisplayName("sitemap.xml — 정상 URL로 loc 포함된 XML 반환")
    void sitemapContainsLoc() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://www.kraft.io.kr");

        var response = controller.sitemap();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @Test
    @DisplayName("robots.txt — 정상 URL로 Sitemap 포함된 텍스트 반환")
    void robotsTxtContainsSitemap() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://www.kraft.io.kr");

        var response = controller.robots();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.getBody())
                .contains("User-agent: *")
                .contains("Disallow: /ops")
                .contains("Sitemap: https://www.kraft.io.kr/sitemap.xml");
    }

    @Test
    @DisplayName("후행 슬래시는 제거되어 URL이 정규화된다")
    void trailingSlashIsStripped() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://www.kraft.io.kr///");

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("null·빈·공백 URL은 기본값으로 대체된다")
    void nullOrBlankUrlFallsBackToDefault(String configuredUrl) {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn(configuredUrl);

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "file:///etc/hosts", "javascript:alert(1)"})
    @DisplayName("http/https 외 스킴은 기본값으로 대체된다")
    void nonHttpSchemesFallBackToDefault(String badUrl) {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn(badUrl);

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @Test
    @DisplayName("쿼리 스트링이 있는 URL은 기본값으로 대체된다")
    void urlWithQueryStringFallsBackToDefault() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://example.com?foo=bar");

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @Test
    @DisplayName("프래그먼트가 있는 URL은 기본값으로 대체된다")
    void urlWithFragmentFallsBackToDefault() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://example.com#section");

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @Test
    @DisplayName("userInfo가 있는 URL은 기본값으로 대체된다")
    void urlWithUserInfoFallsBackToDefault() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://user:pass@example.com");

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>https://www.kraft.io.kr/</loc>");
    }

    @Test
    @DisplayName("http 스킴은 허용된다")
    void httpSchemeIsAllowed() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("http://localhost:8080");

        var response = controller.sitemap();

        assertThat(response.getBody()).contains("<loc>http://localhost:8080/</loc>");
    }

    @Test
    @DisplayName("XML 특수문자는 이스케이프된다")
    void xmlSpecialCharsAreEscaped() {
        when(environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr"))
                .thenReturn("https://www.kraft.io.kr");

        var response = controller.sitemap();

        String body = response.getBody();
        assertThat(body).doesNotContain("<loc>https://www.kraft.io.kr&</loc>");
        assertThat(body).contains("</urlset>");
    }
}
