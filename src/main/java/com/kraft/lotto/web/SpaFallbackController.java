package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA(Next.js) 클라이언트 라우팅 지원 — API·관리자·정적 리소스 외 모든 경로를 index.html 로 위임한다.
 */
@Controller
public class SpaFallbackController {

    @GetMapping(value = {
            "/{path:[^\\.]*}",
            "/{path:[^\\.]*}/{sub:[^\\.]*}",
            "/{path:[^\\.]*}/{sub:[^\\.]*}/{deep:[^\\.]*}",
    })
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.startsWith("/admin/")
                || uri.startsWith("/actuator/") || uri.equals("/sitemap.xml")
                || uri.equals("/robots.txt")) {
            return null;
        }
        return "forward:/index.html";
    }
}
