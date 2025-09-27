package com.example.GoCafe.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String DEFAULT_MESSAGE = "로그인이 필요한 서비스입니다.";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException)
            throws IOException, ServletException {

        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        boolean wantsJson =
                (accept != null && accept.contains("application/json"))
                        || uri.startsWith("/api")
                        || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        if (wantsJson) {
            // JSON 응답
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String body = """
                    {"status":401,"message":"로그인이 필요한 서비스입니다."}
                    """;
            response.getWriter().write(body);
        } else {
            // 페이지 요청: 로그인 페이지로 보내되 쿼리로 메시지 전달
            String target = request.getRequestURI();
            String q = request.getQueryString();
            if (q != null && !q.isBlank()) target += "?" + q;
            String redirect = "/login?msg=" + java.net.URLEncoder.encode(DEFAULT_MESSAGE, StandardCharsets.UTF_8) +
                    "&redirect=" + java.net.URLEncoder.encode(target, StandardCharsets.UTF_8);
            response.sendRedirect(redirect);
        }
    }
}
