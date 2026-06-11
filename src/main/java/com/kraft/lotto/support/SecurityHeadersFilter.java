package com.kraft.lotto.support;

import com.kraft.lotto.infra.config.KraftAdProperties;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CSP_NONCE_ATTRIBUTE = "cspNonce";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int NONCE_BYTE_LENGTH = 16;

    // AdSense가 요구하는 추가 CSP 지시문
    private static final String ADSENSE_SCRIPT_SRC =
            " https://pagead2.googlesyndication.com https://adservice.google.com";
    private static final String ADSENSE_IMG_SRC =
            " https://*.doubleclick.net https://*.googlesyndication.com";
    private static final String ADSENSE_FRAME_SRC =
            " https://googleads.g.doubleclick.net https://tpc.googlesyndication.com";
    private static final String ADSENSE_CONNECT_SRC =
            " https://pagead2.googlesyndication.com https://adservice.google.com https://*.doubleclick.net";

    private final KraftSecurityProperties securityProperties;
    private final KraftAdProperties adProperties;

    @Autowired
    public SecurityHeadersFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider,
                                  ObjectProvider<KraftAdProperties> adPropertiesProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new),
             adPropertiesProvider.getIfAvailable(KraftAdProperties::new));
    }

    SecurityHeadersFilter(KraftSecurityProperties securityProperties, KraftAdProperties adProperties) {
        this.securityProperties = securityProperties;
        this.adProperties = adProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (securityProperties.getHeaders().isEnabled()) {
            String baseCsp = securityProperties.getHeaders().getContentSecurityPolicy();
            if (adProperties.isEnabled() && !adProperties.getAdsenseClientId().isBlank()) {
                baseCsp = appendAdsenseCsp(baseCsp);
            }

            String csp;
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/admin")) {
                // Thymeleaf 렌더링 경로: nonce를 템플릿에서 사용
                String nonce = generateNonce();
                request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
                csp = injectNonce(baseCsp, nonce);
            } else {
                // React 정적 export 경로: inline <script> 태그에 nonce를 주입할 수 없으므로 'unsafe-inline' 사용
                csp = injectUnsafeInline(baseCsp);
            }

            response.setHeader("Content-Security-Policy", csp);
            response.setHeader("X-Frame-Options", securityProperties.getHeaders().getXFrameOptions());
            response.setHeader("Referrer-Policy", securityProperties.getHeaders().getReferrerPolicy());
            response.setHeader("Permissions-Policy", securityProperties.getHeaders().getPermissionsPolicy());
            response.setHeader("X-Content-Type-Options", "nosniff");
            if (securityProperties.getHeaders().isHstsEnabled()) {
                String hstsValue = "max-age=" + securityProperties.getHeaders().getHstsMaxAgeSeconds()
                        + (securityProperties.getHeaders().isHstsIncludeSubDomains() ? "; includeSubDomains" : "")
                        + (securityProperties.getHeaders().isHstsPreload() ? "; preload" : "");
                response.setHeader("Strict-Transport-Security", hstsValue);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    static String injectNonce(String csp, String nonce) {
        String nonceDirective = "'nonce-" + nonce + "'";
        if (csp.contains("script-src ")) {
            return csp.replace("script-src ", "script-src " + nonceDirective + " ");
        }
        return csp;
    }

    static String injectUnsafeInline(String csp) {
        if (csp.contains("script-src ")) {
            return csp.replace("script-src ", "script-src 'unsafe-inline' ");
        }
        return csp;
    }

    private static String appendAdsenseCsp(String csp) {
        csp = appendToDirective(csp, "script-src",  ADSENSE_SCRIPT_SRC);
        csp = appendToDirective(csp, "img-src",     ADSENSE_IMG_SRC);
        csp = appendToDirective(csp, "frame-src",   ADSENSE_FRAME_SRC);
        csp = appendToDirective(csp, "connect-src", ADSENSE_CONNECT_SRC);
        return csp;
    }

    private static String appendToDirective(String csp, String directive, String addition) {
        String token = directive + " ";
        if (csp.contains(token)) {
            return csp.replace(token, token + addition.stripLeading() + " ");
        }
        return csp;
    }
}
