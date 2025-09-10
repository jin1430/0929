package com.example.GoCafe.controller;

import com.example.GoCafe.dto.MemberForm;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.service.MemberService;
import com.example.GoCafe.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final ReviewService reviewService;

    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         Model model) {
        // 로그인 여부 확인
        if (principal == null) return "redirect:/login";

        // 사용자 조회
        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login"; // 존재하지 않으면 로그인 다시

        // MemberForm은 생성자 대신 setter로 채움
        MemberForm view = new MemberForm();
        view.setId(member.getId());
        view.setEmail(member.getEmail());
        // 비밀번호는 노출 금지
        view.setNickname(member.getNickname());
        view.setAge(member.getAge());
        view.setGender(member.getGender());
        view.setRoleKind(member.getRoleKind());
        view.setCreatedAt(member.getCreatedAt());
        view.setPhoto(member.getPhoto());
        view.setTokenVersion(member.getTokenVersion());
        // member.getFavorites()는 List<Favorite>일 가능성 높음. 필요하면 별도 모델 속성으로 전달

        // 모델 바인딩
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("currentUserNickname", member.getNickname());
        model.addAttribute("member", view);
        model.addAttribute("memberPhoto", member.getPhoto() == null ? "" : member.getPhoto());
        // model.addAttribute("favorites", member.getFavorites()); // 즐겨찾기 목록이 필요하면 사용

        return "member/mypage";
    }


    // 프로필 수정 화면
    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                       Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        // MemberForm은 생성자 대신 setter 사용
        MemberForm view = new MemberForm();
        view.setId(member.getId());
        view.setEmail(member.getEmail());
        // 비밀번호는 노출/세팅 금지
        view.setNickname(member.getNickname());
        view.setAge(member.getAge());
        view.setGender(member.getGender());
        view.setRoleKind(member.getRoleKind());
        view.setCreatedAt(member.getCreatedAt());
        view.setPhoto(member.getPhoto());
        view.setTokenVersion(member.getTokenVersion());

        // 본문 모델
        model.addAttribute("member", view);

        // 사진 파생 값
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("hasPhoto", hasPhoto);

        return "member/edit";
    }

    // 프로필 수정
    @PostMapping("/edit")
    public String editDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         @RequestParam(value = "member_nickname", required = false) String nickname,
                         @RequestParam(value = "member_age", required = false) Long age,
                         @RequestParam(value = "member_gender", required = false) String gender,
                         @RequestParam(value = "member_photo", required = false) String photo,
                         @RequestParam(value = "current_password", required = false) String currentPassword,
                         @RequestParam(value = "new_password", required = false) String newPassword,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        // 로그인 확인
        if (principal == null) return "redirect:/login";

        // 현재 회원 조회
        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 부분 수정은 서비스에서 null 무시 방식으로 처리
        boolean pwChanged = memberService.updateSelf(
                me.getId(), nickname, age, gender, photo, currentPassword, newPassword
        );

        // 비밀번호 변경 시 강제 로그아웃
        if (pwChanged) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?password=changed";
        }
        return "redirect:/member/me?update=success";
    }

    // 탈퇴 확인 화면
    @GetMapping("/withdraw")
    public String withdrawPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               Model model) {
        // 로그인 확인
        if (principal == null) return "redirect:/login";

        // 현재 회원 조회
        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 점표기 회피용 단일 키
        model.addAttribute("email", me.getEmail());
        model.addAttribute("nickname", me.getNickname());
        return "member/withdraw";
    }

    // 탈퇴 실행
    @PostMapping("/withdraw")
    public String withdrawDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        // 로그인 확인
        if (principal == null) return "redirect:/login";

        // 현재 회원 조회
        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 탈퇴 처리
        memberService.withdrawSelf(me.getId());

        // 세션/시큐리티 컨텍스트 정리
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        // PRG
        return "redirect:/?withdraw=success";
    }

    @GetMapping("/reviews")
    public String myReviews(@AuthenticationPrincipal User principal,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        if (principal == null) return "redirect:/login";

        var me = memberService.findByEmail(principal.getUsername());
        var pageable = PageRequest.of(page, 10); // 기본 10개
        var myReviews = reviewService.findMyReviews(me.getId(), pageable);

        model.addAttribute("items", myReviews.getContent());
        model.addAttribute("hasContent", !myReviews.isEmpty());
        model.addAttribute("totalElements", myReviews.getTotalElements());

        // 페이지네이션 데이터
        int current = myReviews.getNumber();
        int totalPages = Math.max(myReviews.getTotalPages(), 1);
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            var m = new HashMap<String, Object>();
            m.put("index", i);
            m.put("display", i + 1);
            m.put("active", i == current);
            pages.add(m);
        }

        model.addAttribute("pages", pages);
        model.addAttribute("hasPrev", myReviews.hasPrevious());
        model.addAttribute("hasNext", myReviews.hasNext());
        model.addAttribute("prevPage", Math.max(0, current - 1));
        model.addAttribute("nextPage", Math.min(totalPages - 1, current + 1));

        // 좌측 네비 활성화
        model.addAttribute("nav_reviews", true);

        return "member/reviews";
    }
}