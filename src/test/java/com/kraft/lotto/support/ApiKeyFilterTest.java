package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("API 키 필터")
class ApiKeyFilterTest {

    private static final String VALID_KEY = "test-api-key-abc123";
    private static final String API_PATH = "/api/v1/rounds/latest";
    private static final String WEB_PATH = "/";

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("인증 비활성화 시 키 없이도 API 요청이 통과한다")
    void disabledFilterPassesThroughWithoutKey() throws Exception {
        ApiKeyFilter filter = filterWithEnabled(false);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("인증 활성화 시 유효한 키로 API 요청이 통과한다")
    void enabledFilterPassesWithValidKey() throws Exception {
        ApiKeyFilter filter = filterWithKeys(VALID_KEY);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        req.addHeader("X-Api-Key", VALID_KEY);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("인증 활성화 시 키가 없으면 401을 반환한다")
    void enabledFilterRejectsMissingKey() throws Exception {
        ApiKeyFilter filter = filterWithKeys(VALID_KEY);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verifyNoInteractions(chain);
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).contains("application/json");

        var body = objectMapper.readTree(res.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("인증 활성화 시 잘못된 키는 401을 반환한다")
    void enabledFilterRejectsInvalidKey() throws Exception {
        ApiKeyFilter filter = filterWithKeys(VALID_KEY);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        req.addHeader("X-Api-Key", "wrong-key");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verifyNoInteractions(chain);
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("/api/v1/ 외 경로는 키 검증 없이 통과한다")
    void nonApiPathSkipsValidation() throws Exception {
        ApiKeyFilter filter = filterWithKeys(VALID_KEY);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(WEB_PATH);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("인증 활성화 시 keys가 비어 있으면 모든 요청이 통과한다")
    void enabledWithEmptyKeysPassesThrough() throws Exception {
        KraftApiKeyProperties props = new KraftApiKeyProperties();
        props.setEnabled(true);
        props.setKeys(List.of());
        ApiKeyFilter filter = new ApiKeyFilter(props, objectMapper);

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("여러 키 중 일치하는 키가 있으면 통과한다")
    void multipleKeysAnyMatchPasses() throws Exception {
        KraftApiKeyProperties props = new KraftApiKeyProperties();
        props.setEnabled(true);
        props.setKeys(List.of("key-one", "key-two", "key-three"));
        ApiKeyFilter filter = new ApiKeyFilter(props, objectMapper);

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        req.addHeader("X-Api-Key", "key-two");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 키 값도 정상 처리한다")
    void keyWithWhitespaceIsTrimmedAndValidated() throws Exception {
        ApiKeyFilter filter = filterWithKeys(VALID_KEY);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestTo(API_PATH);
        req.addHeader("X-Api-Key", "  " + VALID_KEY + "  ");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    private ApiKeyFilter filterWithEnabled(boolean enabled) {
        KraftApiKeyProperties props = new KraftApiKeyProperties();
        props.setEnabled(enabled);
        return new ApiKeyFilter(props, objectMapper);
    }

    private ApiKeyFilter filterWithKeys(String... keys) {
        KraftApiKeyProperties props = new KraftApiKeyProperties();
        props.setEnabled(true);
        props.setKeys(List.of(keys));
        return new ApiKeyFilter(props, objectMapper);
    }

    private static MockHttpServletRequest requestTo(String path) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
        req.setRequestURI(path);
        return req;
    }
}
