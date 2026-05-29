package com.kraft.lotto.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WwwRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String host = request.getServerName();
        if (needsWwwRedirect(host)) {
            String query = request.getQueryString();
            String location = "https://www." + host + request.getRequestURI()
                    + (query != null ? "?" + query : "");
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", location);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean needsWwwRedirect(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (host.startsWith("www.")) {
            return false;
        }
        if (host.equals("localhost") || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.")) {
            return false;
        }
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return false;
        }
        return true;
    }
}
