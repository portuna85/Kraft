package com.kraft.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicApiCacheControlFilter extends OncePerRequestFilter {

    private static final String SHORT_PUBLIC_CACHE = "public, max-age=60, must-revalidate";
    // 회차 번호로 조회하는 과거 회차는 발표 후 불변이므로 더 긴 캐시를 허용한다.
    private static final String LONG_PUBLIC_CACHE = "public, max-age=86400, stale-while-revalidate=3600";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod()) || !isCacheablePath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        boolean notModified = false;
        try {
            filterChain.doFilter(request, responseWrapper);

            if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
                String cacheControl = cacheControlFor(request.getRequestURI());
                responseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
                byte[] body = responseWrapper.getContentAsByteArray();
                String etag = responseWrapper.getHeader(HttpHeaders.ETAG);
                if (body.length > 0 && etag == null) {
                    etag = "\"" + DigestUtils.md5DigestAsHex(body) + "\"";
                    responseWrapper.setHeader(HttpHeaders.ETAG, etag);
                }
                if (etag != null && etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
                    // 304는 본문이 없어야 하므로 캐시된 본문은 wrapper에 버려두고 응답에는 복사하지 않는다.
                    notModified = true;
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
                    response.setHeader(HttpHeaders.ETAG, etag);
                }
            }
        } finally {
            if (!notModified) {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private static String cacheControlFor(String path) {
        return isHistoricalRoundPath(path) ? LONG_PUBLIC_CACHE : SHORT_PUBLIC_CACHE;
    }

    private static boolean isCacheablePath(String path) {
        return path.startsWith("/api/v1/stats/")
                || path.equals("/api/v1/rounds")
                || path.equals("/api/v1/rounds/latest")
                || isHistoricalRoundPath(path);
    }

    private static boolean isHistoricalRoundPath(String path) {
        return path.matches("^/api/v1/rounds/\\d+$");
    }
}
