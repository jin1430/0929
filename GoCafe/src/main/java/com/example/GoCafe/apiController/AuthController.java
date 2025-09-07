package com.example.GoCafe.apiController;

import com.example.GoCafe.dto.MemberForm;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(MemberRepository memberRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberForm body) {
        String email = body.getEmail();
        String pw = body.getPassword();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email/password required"));
        }
        if (memberRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already exists"));
        }

        Member m = new Member();
        m.setEmail(email);
        m.setPassword(passwordEncoder.encode(pw)); // bcrypt
        m.setNickname(body.getNickname());
        m.setAge(body.getAge());
        m.setGender(body.getGender());
        m.setRoleKind((body.getRoleKind() == null || body.getRoleKind().isBlank()) ? "USER" : body.getRoleKind());
        m.setPhoto(body.getPhoto());
        m.setTokenVersion(0L);

        memberRepository.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberForm body) {
        String email = body.getEmail();
        String pw = body.getPassword();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email/password required"));
        }

        return memberRepository.findByEmail(email)
                .map(member -> {
                    boolean matches = false;
                    if (member.getPassword() != null) {
                        try {
                            matches = passwordEncoder.matches(pw, member.getPassword());
                        } catch (Exception ignored) {}
                        // 개발 편의: bcrypt가 아니면 평문 비교 한 번 허용(운영에서는 제거 권장)
                        if (!matches) matches = pw.equals(member.getPassword());
                    }
                    if (!matches) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Invalid credentials"));
                    }

                    var ud = User
                            .withUsername(member.getEmail())
                            .password(member.getPassword())
                            .authorities(
                                    (member.getRoleKind()!=null && member.getRoleKind().startsWith("ROLE_"))
                                            ? member.getRoleKind()
                                            : "ROLE_" + (member.getRoleKind()==null ? "USER" : member.getRoleKind())
                            )
                            .build();

                    String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion());

                    // ✅ HttpOnly 쿠키에도 토큰 심기 (SSR 헤더 토글을 위해)
                    ResponseCookie cookie = ResponseCookie.from("AT", token)
                            .httpOnly(true)
                            .secure(false)       // 프로덕션/HTTPS에서는 true 권장
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(Duration.ofDays(7))
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(Map.of("tokenType","Bearer","token", token));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        // ✅ 쿠키 삭제(무효화)는 인증 여부와 관계없이 항상 수행
        ResponseCookie del = ResponseCookie.from("AT", "")
                .httpOnly(true)
                .secure(false) // prod: true
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        // 인증 안 되었으면 쿠키만 제거하고 204
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, del.toString())
                    .build();
        }

        // 인증된 경우: 서버측 무효화를 위해 tokenVersion 증가
        String email = auth.getName();
        return memberRepository.findByEmail(email)
                .map(m -> {
                    long nv = (m.getTokenVersion()==null?0L:m.getTokenVersion()) + 1L;
                    m.setTokenVersion(nv);
                    memberRepository.save(m);
                    return ResponseEntity.noContent()
                            .header(HttpHeaders.SET_COOKIE, del.toString())
                            .build();
                })
                // 유저를 못 찾았어도 쿠키는 제거
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.SET_COOKIE, del.toString())
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();
        return memberRepository.findByEmail(email)
                .<ResponseEntity<?>>map(m -> {
                    MemberForm out = new MemberForm();
                    out.setId(m.getId());
                    out.setEmail(m.getEmail());
                    // 비밀번호는 응답에 노출하지 않음
                    out.setNickname(m.getNickname());
                    out.setAge(m.getAge());
                    out.setGender(m.getGender());
                    out.setRoleKind(m.getRoleKind());
                    out.setCreatedAt(m.getCreatedAt()); // MemberForm에 createdAt 필드가 있을 때만 유지
                    out.setPhoto(m.getPhoto());
                    out.setTokenVersion(m.getTokenVersion());
                    return ResponseEntity.ok(out);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found")));
    }


}
