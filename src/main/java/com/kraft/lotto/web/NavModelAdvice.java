package com.kraft.lotto.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavModelAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return (path != null) ? path : "/";
    }
}
