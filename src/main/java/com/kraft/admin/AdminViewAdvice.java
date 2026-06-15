package com.kraft.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current request path to admin views.
 *
 * Thymeleaf 3.1 removed the {@code #httpServletRequest} (and {@code #request},
 * {@code #session}, ...) expression utility objects for security reasons, so the
 * active-nav highlighting in {@code admin/layout.html} reads {@code currentPath}
 * from the model instead.
 */
@ControllerAdvice(assignableTypes = AdminController.class)
public class AdminViewAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
