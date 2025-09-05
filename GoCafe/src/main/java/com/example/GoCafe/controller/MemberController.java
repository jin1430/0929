// src/main/java/com/example/GoCafe/controller/MemberController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.dto.MemberForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal User principal, Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        MemberForm view = new MemberForm(
                member.getMemberId(),
                member.getMemberEmail(),
                (String) null, // 비밀번호 노출 금지
                member.getMemberNickname(),
                member.getMemberAge(),
                member.getMemberGender(),
                member.getMemberRole(),
                member.getMemberDate(),
                member.getMemberPhoto(),
                member.getTokenVersion(),
                (Cafe) member.getFavorites()
        );

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("currentUserNickname", member.getMemberNickname());
        model.addAttribute("member", view);
        model.addAttribute("memberPhoto", member.getMemberPhoto() == null ? "" : member.getMemberPhoto());

        return "member/mypage";
    }

    // ====== 프로필 수정 화면 ======
    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal User principal, Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        MemberForm view = new MemberForm(
                member.getMemberId(),
                member.getMemberEmail(),
                (String) null,
                member.getMemberNickname(),
                member.getMemberAge(),
                member.getMemberGender(),
                member.getMemberRole(),
                member.getMemberDate(),
                member.getMemberPhoto(),
                member.getTokenVersion(),
                (Cafe) member.getFavorites()
        );

        // 본문 모델
        model.addAttribute("member", view);

        // ★ 사진 파생 값: 템플릿에서 memberPhoto를 쓰지 않도록 최상위 키로 제공
        String photoName = member.getMemberPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("hasPhoto", hasPhoto);

        return "member/edit";
    }


    // ====== 프로필 수정 + (옵션) 비번 변경 ======
    @PostMapping("/edit")
    public String editDo(@AuthenticationPrincipal User principal,
                         @RequestParam(value = "member_nickname", required = false) String nickname,
                         @RequestParam(value = "member_age", required = false) Long age,
                         @RequestParam(value = "member_gender", required = false) String gender,
                         @RequestParam(value = "member_photo", required = false) String photo,
                         @RequestParam(value = "current_password", required = false) String currentPassword,
                         @RequestParam(value = "new_password", required = false) String newPassword,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Long myId = memberService.findByEmail(principal.getUsername()).getMemberId();
        boolean pwChanged = memberService.updateSelf(
                myId, nickname, age, gender, photo, currentPassword, newPassword
        );

        // 비번을 바꿨다면 보안을 위해 재로그인 유도
        if (pwChanged) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?password=changed";
        }
        return "redirect:/member/me?update=success";
    }

    // --- 탈퇴 확인 화면 ---
    @GetMapping("/withdraw")
    public String withdrawPage(@AuthenticationPrincipal User principal, Model model) {
        if (principal == null) return "redirect:/login";
        Member me = memberService.findByEmail(principal.getUsername());
        model.addAttribute("email", me.getMemberEmail());   // 점표기 회피용 단일 키
        model.addAttribute("nickname", me.getMemberNickname());
        return "member/withdraw"; // templates/member/withdraw.mustache
    }

    // --- 실제 탈퇴 실행 ---
    @PostMapping("/withdraw")
    public String withdrawDo(@AuthenticationPrincipal User principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Long myId = memberService.findByEmail(principal.getUsername()).getMemberId();
        memberService.withdrawSelf(myId);

        // 세션/시큐리티 컨텍스트 정리
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // PRG: 홈으로 리다이렉트
        return "redirect:/?withdraw=success";
    }
}
