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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod()) || !isCacheablePath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);

            if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
                responseWrapper.setHeader(HttpHeaders.CACHE_CONTROL, SHORT_PUBLIC_CACHE);
                byte[] body = responseWrapper.getContentAsByteArray();
                if (body.length > 0 && responseWrapper.getHeader(HttpHeaders.ETAG) == null) {
                    responseWrapper.setHeader(HttpHeaders.ETAG, "\"" + DigestUtils.md5DigestAsHex(body) + "\"");
                }
            }
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private static boolean isCacheablePath(String path) {
        return path.startsWith("/api/v1/stats/")
                || path.equals("/api/v1/rounds")
                || path.equals("/api/v1/rounds/latest")
                || path.matches("^/api/v1/rounds/\\d+$");
    }
}
