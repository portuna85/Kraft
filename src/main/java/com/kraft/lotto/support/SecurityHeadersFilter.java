package com.kraft.lotto.support;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final KraftSecurityProperties securityProperties;

    @Autowired
    public SecurityHeadersFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new));
    }

    SecurityHeadersFilter(KraftSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (securityProperties.getHeaders().isEnabled()) {
            response.setHeader("Content-Security-Policy", securityProperties.getHeaders().getContentSecurityPolicy());
            response.setHeader("X-Frame-Options", securityProperties.getHeaders().getXFrameOptions());
            response.setHeader("Referrer-Policy", securityProperties.getHeaders().getReferrerPolicy());
            response.setHeader("Permissions-Policy", securityProperties.getHeaders().getPermissionsPolicy());
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        filterChain.doFilter(request, response);
    }
}
