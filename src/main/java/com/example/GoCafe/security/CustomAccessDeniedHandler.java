package com.example.GoCafe.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        boolean wantsJson =
                (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))
                        || request.getRequestURI().startsWith("/api")
                        || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (wantsJson) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"status":403,"message":"접근 권한이 없습니다."}
            """);
        } else {
            response.sendRedirect("/error/403?msg=" +
                    java.net.URLEncoder.encode("접근 권한이 없습니다.", StandardCharsets.UTF_8));
        }
    }
}
