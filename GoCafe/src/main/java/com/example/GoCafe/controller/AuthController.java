package com.example.GoCafe.controller;

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

    @GetMapping("/login")
    public String loginPage(@RequestParam(value="error", required=false) String error,
                            @RequestParam(value="redirect", required=false) String redirect,
                            Model model) {
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        if (redirect != null && !redirect.isBlank()) model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("memberEmail") String email,
                              @RequestParam("memberPassword") String password,
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

        var ud = User
                .withUsername(member.getEmail())
                .password(member.getPassword())
                .authorities(
                        (member.getRoleKind()!=null && member.getRoleKind().startsWith("ROLE_"))
                                ? member.getRoleKind()
                                : "ROLE_" + (member.getRoleKind()==null ? "USER" : member.getRoleKind())
                )
                .build();

        String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion()==null?0L:member.getTokenVersion());
        ResponseCookie cookie = ResponseCookie.from("AT", token)
                .httpOnly(true).secure(false).sameSite("Lax")
                .path("/").maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return "redirect:/";
    }

    @GetMapping("/signup")
    public String signupPage() { return "auth/signup"; }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String memberEmail,
                               @RequestParam String memberPassword,
                               @RequestParam String memberPasswordConfirm,
                               @RequestParam String memberNickname,
                               @RequestParam(required = false) Long memberAge,
                               @RequestParam(required = false) String memberGender,
                               Model model) {

        if (!memberPassword.equals(memberPasswordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "signup";
        }

        // 이메일 중복 확인
        if (memberRepository.findByEmail(memberEmail).isPresent()) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "signup";
        }

        // 닉네임 중복 확인 → 실패 처리
        if (memberRepository.findByNickname(memberNickname).isPresent()) {
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return "signup";
        }

        Member m = new Member();
        m.setEmail(memberEmail);
        m.setPassword(passwordEncoder.encode(memberPassword));
        m.setNickname(memberNickname);
        m.setAge(memberAge);
        m.setGender("M".equalsIgnoreCase(memberGender) ? "M" :
                "F".equalsIgnoreCase(memberGender) ? "F" : null);
        m.setRoleKind("USER");
        m.setCreatedAt(LocalDateTime.now());
        m.setTokenVersion(0L);

        memberRepository.save(m);
        return "redirect:/login"; // 성공 시 로그인 페이지로 이동
    }

    private String ensureUniqueNickname(String base) {
        String candidate = base;
        int counter = 1;
        while (memberRepository.findByNickname(candidate).isPresent()) {
            if (candidate.length() > 6) {
                // 최대 8자 제약 있으므로 앞쪽 잘라줌
                candidate = base.substring(0, 6) + counter;
            } else {
                candidate = base + counter;
            }
            counter++;
        }
        return candidate;
    }
}
