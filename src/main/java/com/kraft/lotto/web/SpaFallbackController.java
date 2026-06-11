package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA(Next.js) 클라이언트 라우팅 지원.
 * 요청 경로에 해당하는 정적 HTML(Next.js output: export의 페이지별 index.html)이 있으면
 * 그 파일을 서빙하고, 없으면 루트 index.html 을 폴백으로 반환한다.
 */
@Controller
public class SpaFallbackController {

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
        // Next.js trailingSlash: true 로 빌드하므로 /path/index.html 구조
        String pagePath = uri.endsWith("/") ? uri + "index.html" : uri + "/index.html";
        Resource pageHtml = resourceLoader.getResource("classpath:/static" + pagePath);
        if (pageHtml.exists()) {
            return "forward:" + pagePath;
        }
        return "forward:/index.html";
    }
}
