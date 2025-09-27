// src/main/java/com/example/GoCafe/security/SecurityConfig.java
package com.example.GoCafe.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final XssSanitizingFilter xssSanitizingFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService,
            XssSanitizingFilter xssSanitizingFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.xssSanitizingFilter = xssSanitizingFilter;
    }

    // 정적 리소스는 보안 필터 체인에서 완전 제외 (JWT/XSS 등 아예 안탐)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/favicon.ico",
                "/webjars/**",
                "/css/**", "/js/**",
                "/images/**", "/img/**",
                "/uploads/**", "/files/**"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        // JS SDK
                                        "script-src 'self' https://dapi.kakao.com https://t1.daumcdn.net 'unsafe-inline'; " +
                                        // 이미지: self/data/blob/https 허용
                                        "img-src 'self' data: blob: https:; " +
                                        // XHR
                                        "connect-src 'self' https://dapi.kakao.com https://*.daumcdn.net; " +
                                        // 스타일/폰트
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "font-src 'self' https:; " +
                                        "base-uri 'self'; frame-ancestors 'self'; object-src 'none'; " +
                                        "upgrade-insecure-requests;"
                        ))
                        .frameOptions(fo -> fo.sameOrigin())
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
                        .contentTypeOptions(Customizer.withDefaults())
                )
                // 401/403 커스텀 (API는 JSON, 페이지는 리다이렉트)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> handle401(req, res))
                        .accessDeniedHandler((req, res, ex) -> handle403(req, res))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 비로그인 접근 허용
                        .requestMatchers(
                                "/", "/index/**", "/search/**",
                                "/signup", "/login",
                                "/main",
                                "/mission", "/missions/**",
                                "/cafes/**", "/cafe",
                                "/api/recommend" // 인트로 데모 API
                        ).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico").permitAll()
                        .requestMatchers("/uploads/**", "/files/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // 보호 구간
                        .requestMatchers("/api/private/**").authenticated()
                        // NOTE: 기존 정책 유지 — 필요 시 tighten 하세요
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/**").permitAll()

                        // 리뷰 작성은 인증 요구
                        .requestMatchers("/reviews/**").authenticated()

                        .anyRequest().authenticated()
                )
                // 필터 순서: XSS -> JWT -> 나머지
                .addFilterBefore(xssSanitizingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ===== 커스텀 핸들러 =====
    private void handle401(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        boolean wantsJson =
                acceptJson(request) || isApi(request) || isAjax(request);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        if (wantsJson) {
            // JSON 바디
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":401,\"message\":\"로그인이 필요한 서비스입니다.\"}");
        } else {
            // 로그인 페이지 리다이렉트(+ 안내 메시지 + 원래 위치)
            String target = request.getRequestURI();
            String q = request.getQueryString();
            if (q != null && !q.isBlank()) target += "?" + q;

            String msg = URLEncoder.encode("로그인이 필요한 서비스입니다.", StandardCharsets.UTF_8);
            String redir = URLEncoder.encode(target, StandardCharsets.UTF_8);
            response.sendRedirect("/login?msg=" + msg + "&redirect=" + redir);
        }
    }

    private void handle403(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        boolean wantsJson =
                acceptJson(request) || isApi(request) || isAjax(request);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (wantsJson) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":403,\"message\":\"접근 권한이 없습니다.\"}");
        } else {
            String msg = URLEncoder.encode("접근 권한이 없습니다.", StandardCharsets.UTF_8);
            response.sendRedirect("/error/403?msg=" + msg);
        }
    }

    private boolean isApi(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri != null && uri.startsWith("/api");
    }

    private boolean acceptJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private boolean isAjax(HttpServletRequest req) {
        String h = req.getHeader("X-Requested-With");
        return h != null && "XMLHttpRequest".equalsIgnoreCase(h);
    }
    // ===== /커스텀 핸들러 =====

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(List.of("*")); // dev only
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
