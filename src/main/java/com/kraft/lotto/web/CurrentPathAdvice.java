package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 @Controller (Thymeleaf 뷰 반환) 에 currentPath 모델 속성을 자동 주입.
 * base.html 에서 isAdmin 판단 등 경로 기반 조건 처리에 사용.
 */
@ControllerAdvice
public class CurrentPathAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
