package com.example.GoCafe.controller;

import com.example.GoCafe.domain.RoleKind;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
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

    /** 로그인 페이지: 상단 배너(msg) + redirect 쿼리 반영 */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value="error", required=false) String error,
                            @RequestParam(value="msg", required=false) String msg,
                            @RequestParam(value="redirect", required=false) String redirect,
                            Model model) {

        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        if (msg != null && !msg.isBlank())     model.addAttribute("msg", msg);
        if (redirect != null && !redirect.isBlank()) model.addAttribute("redirect", redirect);

        return "auth/login"; // 템플릿 경로
    }

    /** 로그인 처리: 성공 시 안전한 redirect 로 이동 (없으면 / 또는 /main) */
    @PostMapping("/login")
    public String loginSubmit(@RequestParam("memberEmail") String email,
                              @RequestParam("memberPassword") String password,
                              @RequestParam(value="redirect", required=false) String redirect,
                              HttpServletResponse response,
                              RedirectAttributes ra,
                              Model model) {
        Optional<Member> memberTryingLogin = memberRepository.findByEmail(email);
        if (memberTryingLogin.isEmpty()) {
            ra.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/login";
        }
        Member member = memberTryingLogin.get();
        boolean matches = false;
        if (member.getPassword() != null) {
            try { matches = passwordEncoder.matches(password, member.getPassword()); } catch (Exception ignored) {}
            if (!matches) matches = password.equals(member.getPassword()); // dev only
        }
        if (!matches) {
            ra.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/login";
        }

        String roleName = (member.getRoleKind() == null) ? "USER" : member.getRoleKind().name();
        if (roleName.startsWith("ROLE_")) roleName = roleName.substring(5);

        var ud = User.withUsername(member.getEmail())
                .password(member.getPassword())
                .roles(roleName)
                .build();

        String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion()==null?0L:member.getTokenVersion());
        ResponseCookie cookie = ResponseCookie.from("AT", token)
                .httpOnly(true).secure(false).sameSite("Lax")
                .path("/").maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // ✅ 안전한 redirect만 허용 (절대 URL, 스킴, 외부 도메인 차단)
        String target = normalizeRedirect(redirect);
        return "redirect:" + target;
    }

    /** 회원가입 페이지/처리 (기존 그대로) */
    @GetMapping("/signup")
    public String signupPage() { return "auth/signup"; }

    @PostMapping("/signup")
    public String signupSubmit(
            @RequestParam("email") String memberEmail,
            @RequestParam("password") String memberPassword,
            @RequestParam("passwordConfirm") String memberPasswordConfirm,
            @RequestParam("nickname") String memberNickname,
            @RequestParam("age") Integer memberAge,
            @RequestParam("gender") String memberGender,
            Model model) {

        if (!memberPassword.equals(memberPasswordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "auth/signup";
        }
        if (memberRepository.findByEmail(memberEmail).isPresent()) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "auth/signup";
        }
        if (memberRepository.findByNickname(memberNickname).isPresent()) {
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return "auth/signup";
        }

        Member m = new Member();
        m.setEmail(memberEmail);
        m.setPassword(passwordEncoder.encode(memberPassword));
        m.setNickname(memberNickname);
        m.setAge(memberAge == null ? null : memberAge.longValue());
        m.setGender(("M".equalsIgnoreCase(memberGender) || "F".equalsIgnoreCase(memberGender)) ? memberGender.toUpperCase() : null);
        m.setRoleKind(RoleKind.valueOf("USER"));
        m.setCreatedAt(LocalDateTime.now());
        m.setTokenVersion(0L);

        memberRepository.save(m);
        return "redirect:/login";
    }

    // ===== helpers =====

    /** redirect 파라미터를 검증/정규화: 외부로 나가지 않게 하고 기본값 제공 */
    private String normalizeRedirect(String redirect) {
        // 기본 이동 경로: 메인이 더 자연스러우면 "/main"으로 바꿔도 됨
        String fallback = "/";

        if (redirect == null || redirect.isBlank()) return fallback;

        // 절대 URL 차단
        try {
            URI uri = new URI(redirect);
            if (uri.isAbsolute()) return fallback; // http(s)://... 금지
        } catch (URISyntaxException e) {
            return fallback;
        }

        // CR/LF 같은 이상문자 차단
        if (redirect.contains("\n") || redirect.contains("\r")) return fallback;

        // 화이트리스트(선택): 필요한 경우 패턴으로 더 좁힐 수 있음
        if (!redirect.startsWith("/")) return fallback;

        return redirect;
    }

    // 필요 시 닉네임 유니크 생성 유틸 등…
}
