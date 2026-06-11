package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * SPA(Next.js) client routes support.
 * If a matching static HTML export exists, forward to it; otherwise forward to the public index page.
 */
@Controller
public class SpaFallbackController {

    private static final Set<String> RESERVED_PREFIXES = Set.of("admin", "ops", "actuator", "api");

    private final ResourceLoader resourceLoader;

    public SpaFallbackController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(value = {
            "/{path:[^\\.]*}",
            "/{path:[^\\.]*}/{sub:[^\\.]*}",
            "/{path:[^\\.]*}/{sub:[^\\.]*}/{deep:[^\\.]*}",
    })
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (RESERVED_PREFIXES.contains(firstSegment(uri))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String pagePath = uri.endsWith("/") ? uri + "index.html" : uri + "/index.html";
        Resource pageHtml = resourceLoader.getResource("classpath:/static" + pagePath);
        if (pageHtml.exists()) {
            return "forward:" + pagePath;
        }
        return "forward:/index.html";
    }

    private static String firstSegment(String uri) {
        if (uri == null || uri.isBlank() || "/".equals(uri)) {
            return "";
        }
        String normalized = uri.startsWith("/") ? uri.substring(1) : uri;
        int slash = normalized.indexOf('/');
        return slash >= 0 ? normalized.substring(0, slash) : normalized;
    }
}
