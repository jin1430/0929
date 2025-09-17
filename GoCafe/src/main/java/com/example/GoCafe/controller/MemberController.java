package com.example.GoCafe.controller;

import com.example.GoCafe.dto.MemberForm;
import com.example.GoCafe.dto.MyReviewItem;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.service.MemberService;
import com.example.GoCafe.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final ReviewService reviewService;

    /* =========================
     *        MY PAGE
     * ========================= */
    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        // 공통 헤더/레이아웃에서 쓰는 값 + PRO 여부
        populateCommonUserModel(model, member);

        // 진행도(없어도 항상 키 존재하게 세팅)
        Map<String, Object> proProgress = buildProProgress(member.getId(), isPro(member));
        model.addAttribute("proProgress", proProgress);

        // 본문 모델
        model.addAttribute("member", view);
        model.addAttribute("memberPhoto", member.getPhoto() == null ? "" : member.getPhoto());

        // 좌측 네비 활성화
        model.addAttribute("nav_me", true);

        return "member/mypage";
    }

    /* =========================
     *     PROFILE EDIT (GET)
     * ========================= */
    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                       Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        // 공통 키
        populateCommonUserModel(model, member);

        // 사진 파생 값
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("hasPhoto", hasPhoto);

        // 본문 모델
        model.addAttribute("member", view);

        // 네비
        model.addAttribute("nav_edit", true);

        return "member/edit";
    }

    /* =========================
     *     PROFILE EDIT (POST)
     * ========================= */
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
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        boolean pwChanged = memberService.updateSelf(
                me.getId(), nickname, age, gender, photo, currentPassword, newPassword
        );

        if (pwChanged) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?password=changed";
        }
        return "redirect:/member/me?update=success";
    }

    /* =========================
     *       WITHDRAW (GET)
     * ========================= */
    @GetMapping("/withdraw")
    public String withdrawPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 공통 키
        populateCommonUserModel(model, me);

        // 점표기 회피용 단일 키
        model.addAttribute("email", me.getEmail());
        model.addAttribute("nickname", me.getNickname());

        // 네비
        model.addAttribute("nav_withdraw", true);

        return "member/withdraw";
    }

    /* =========================
     *       WITHDRAW (POST)
     * ========================= */
    @PostMapping("/withdraw")
    public String withdrawDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        memberService.withdrawSelf(me.getId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        return "redirect:/?withdraw=success";
    }

    /* =========================
     *        MY REVIEWS
     * ========================= */
    @GetMapping("/reviews")
    public String myReviews(@AuthenticationPrincipal User principal,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        Page<MyReviewItem> myReviews = reviewService.findMyReviews(me.getId(), PageRequest.of(page, 10));

        // 공통 키
        populateCommonUserModel(model, me);

        model.addAttribute("items", myReviews.getContent());
        model.addAttribute("hasContent", !myReviews.isEmpty());
        model.addAttribute("totalElements", myReviews.getTotalElements());

        // 페이지네이션
        int current = myReviews.getNumber();
        int totalPages = Math.max(myReviews.getTotalPages(), 1);
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Map<String, Object> m = new HashMap<>();
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

        // 네비
        model.addAttribute("nav_reviews", true);

        return "member/reviews";
    }

    /* =========================
     *         HELPERS
     * ========================= */

    private MemberForm toForm(Member member) {
        MemberForm view = new MemberForm();
        view.setId(member.getId());
        view.setEmail(member.getEmail());
        view.setNickname(member.getNickname());
        view.setAge(member.getAge());
        view.setGender(member.getGender());
        view.setRoleKind(member.getRoleKind());
        view.setCreatedAt(member.getCreatedAt());
        view.setPhoto(member.getPhoto());
        view.setTokenVersion(member.getTokenVersion());
        return view;
    }

    /** 헤더/레이아웃에서 공통으로 쓰는 키들을 항상 채워준다. */
    private void populateCommonUserModel(Model model, Member member) {
        boolean isPro = isPro(member);
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("memberNickname", member.getNickname());   // ✅ 헤더가 참조
        model.addAttribute("currentUserNickname", member.getNickname());
        model.addAttribute("isPro", isPro);

        // 사진 관련 키(헤더/페이지 어디서든 사용 가능)
        model.addAttribute("hasPhoto", hasPhoto);
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("memberPhoto", hasPhoto ? photoName : "");
    }

    private boolean isPro(Member m) {
        return m != null && "PRO".equalsIgnoreCase(m.getRoleKind());
    }

    /**
     * PRO가 아니더라도 템플릿에서 항상 사용할 수 있도록
     * total/good/percent 키를 포함한 맵을 돌려줍니다.
     */
    private Map<String, Object> buildProProgress(Long memberId, boolean isPro) {
        final int TOTAL_TARGET = 10;
        final int GOOD_TARGET  = 6;

        long totalReviews = 0L;
        long goodReviews  = 0L;

        try {
            Page<MyReviewItem> p = reviewService.findMyReviews(memberId, PageRequest.of(0, 1));
            totalReviews = (p != null) ? p.getTotalElements() : 0L;
        } catch (Throwable ignore) {}

        try {
            List<Review> all = reviewService.findAll();
            goodReviews = all.stream()
                    .filter(r -> r.getMember() != null && Objects.equals(r.getMember().getId(), memberId))
                    .filter(r -> "GOOD".equalsIgnoreCase(String.valueOf(r.getSentiment())))
                    .count();
        } catch (Throwable ignore) {}

        int pctTotal = (int) Math.min(100, Math.round((totalReviews * 100.0) / TOTAL_TARGET));
        int pctGood  = (int) Math.min(100, Math.round((goodReviews  * 100.0) / GOOD_TARGET));
        int percent  = Math.min(pctTotal, pctGood);
        if (isPro) percent = 100;

        Map<String, Object> map = new HashMap<>();
        map.put("totalReviews", totalReviews);
        map.put("goodReviews",  goodReviews);
        map.put("percent",      percent);
        return map;
    }
}
