package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavModelAdvice {

    private static final Map<String, String> PATH_LABELS = Map.of(
            "/",          "번호 추천",
            "/recommend", "번호 추천",
            "/latest",    "최신 당첨번호",
            "/rounds",    "회차별 당첨번호",
            "/frequency", "번호 빈도 통계",
            "/stats",     "패턴 통계",
            "/companion", "동반 번호",
            "/analysis",  "분석 도구",
            "/faq",       "자주 묻는 질문"
    );

    private static final String BREADCRUMB_HOME =
            "{\"@context\":\"https://schema.org\",\"@type\":\"BreadcrumbList\","
            + "\"itemListElement\":[{\"@type\":\"ListItem\",\"position\":1,"
            + "\"name\":\"홈\",\"item\":\"%s/\"}]}";

    private static final String BREADCRUMB_SUB =
            "{\"@context\":\"https://schema.org\",\"@type\":\"BreadcrumbList\","
            + "\"itemListElement\":[{\"@type\":\"ListItem\",\"position\":1,"
            + "\"name\":\"홈\",\"item\":\"%s/\"},{\"@type\":\"ListItem\","
            + "\"position\":2,\"name\":\"%s\",\"item\":\"%s%s\"}]}";

    @Value("${kraft.public-base-url:https://www.kraft.io.kr}")
    private String baseUrl;

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return (path != null) ? path : "/";
    }

    @ModelAttribute("breadcrumbJsonLd")
    public String breadcrumbJsonLd(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.startsWith("/admin") || path.startsWith("/actuator")) {
            return null;
        }
        String label = PATH_LABELS.get(path);
        if (label == null) {
            return null;
        }
        if ("/".equals(path) || "/recommend".equals(path)) {
            return BREADCRUMB_HOME.formatted(baseUrl);
        }
        return BREADCRUMB_SUB.formatted(baseUrl, label, baseUrl, path);
    }
}
