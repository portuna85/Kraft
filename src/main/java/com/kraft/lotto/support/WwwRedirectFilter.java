package com.kraft.lotto.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WwwRedirectFilter extends OncePerRequestFilter {

    private static final String APEX_HOST = "kraft.io.kr";
    private static final String CANONICAL_ORIGIN = "https://www.kraft.io.kr";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (needsWwwRedirect(request.getServerName())) {
            String query = request.getQueryString();
            String location = CANONICAL_ORIGIN + stripCrLf(request.getRequestURI())
                    + (query != null && !query.isBlank() ? "?" + stripCrLf(query) : "");
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", location);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean needsWwwRedirect(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        int colon = h.indexOf(':');
        if (colon >= 0) {
            h = h.substring(0, colon);
        }
        return APEX_HOST.equals(h);
    }

    private static String stripCrLf(String value) {
        return value.replace("\r", "").replace("\n", "");
    }
}
