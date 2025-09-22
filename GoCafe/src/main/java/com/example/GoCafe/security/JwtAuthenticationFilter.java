package com.example.GoCafe.security;

import com.example.GoCafe.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AT_COOKIE = "AT";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            // 공개 경로는 스킵
            if (shouldSkip(req)) {
                chain.doFilter(req, res);
                return;
            }

            String token = resolveToken(req);

            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = tokenProvider.extractUsername(token); // 만료/위조시 예외
                Number verClaim = tokenProvider.extractClaim(token, claims -> claims.get("ver", Number.class));

                var opt = memberRepository.findByEmail(email);
                if (opt.isPresent()) {
                    long currentVer = opt.get().getTokenVersion() == null ? 0L : opt.get().getTokenVersion();
                    long tokenVer   = verClaim == null ? 0L : verClaim.longValue();
                    if (currentVer != tokenVer) {
                        log.debug("JWT 버전 불일치: tokenVer={}, currentVer={}", tokenVer, currentVer);
                        clearAccessTokenCookie(res);
                        chain.doFilter(req, res);
                        return;
                    }

                    UserDetails ud = userDetailsService.loadUserByUsername(email);
                    var authToken = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT 인증 성공: {}", email);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.debug("만료된 JWT: {}", ex.getMessage());
            clearAccessTokenCookie(res); // 반복 실패 방지
        } catch (Exception e) {
            log.debug("JWT 처리 실패: {}", e.getMessage());
        }
        chain.doFilter(req, res);
    }

    private boolean shouldSkip(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.equals("/login")
                || uri.equals("/signup")
                || uri.equals("/api/auth/login")
                || uri.equals("/api/auth/signup")
                || uri.equals("/api/auth/logout")  // 원하면 공개 유지
                || uri.startsWith("/public")
                || uri.startsWith("/h2-console")
                || uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")
                || uri.startsWith("/static") || uri.startsWith("/assets");
        // 홈("/")을 인증 상태에 따라 SSR로 바꾸고 싶으면 여기서 빼두는 게 좋습니다.
    }


    private String resolveToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = auth.substring(7).trim();
            if (StringUtils.hasText(token)) return token;
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AT_COOKIE.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void clearAccessTokenCookie(HttpServletResponse res) {
        Cookie c = new Cookie(AT_COOKIE, "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0);
        res.addCookie(c);
    }
}
