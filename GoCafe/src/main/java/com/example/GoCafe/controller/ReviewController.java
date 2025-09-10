package com.example.GoCafe.controller;

import com.example.GoCafe.dto.ReviewForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.repository.ReviewTagRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final MemberRepository memberRepository;
    private final CafeRepository cafeRepository;


    // 폴백 페이지(팝업이 안 뜰 때만 노출). view name은 절대 슬래시 X
    @GetMapping("/new")
    public String newForm(@RequestParam("cafeId") Long cafeId) {
        // 브라우저 주소: /cafes/{id}#reviewModal  => CSS :target로 즉시 오픈
        return "redirect:/cafes/" + cafeId + "#reviewModal";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute ReviewForm form,
                         @RequestParam(value = "photos", required = false) MultipartFile[] photos, // 파일 인풋이 있어도 안전하게 받기
                         RedirectAttributes ra,
                         Authentication authentication,
                         HttpServletRequest request) {

        // 0) 인증 보장
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login?error=" + URLEncoder.encode("로그인이 필요한 서비스입니다.", StandardCharsets.UTF_8)
                    + "&redirect=" + URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
        }

        // 1) cafeId 보정
        if (form.getCafeId() == null) {
            String fromMain = request.getParameter("cafeId"); // hidden이나 쿼리스트링 보정
            if (fromMain != null && !fromMain.isBlank()) {
                form.setCafeId(Long.valueOf(fromMain));
            } else {
                String ref = request.getHeader("Referer");
                if (ref != null) {
                    Matcher m = Pattern.compile("/cafes/(\\d+)").matcher(ref);
                    if (m.find()) form.setCafeId(Long.valueOf(m.group(1)));
                }
            }
            if (form.getCafeId() == null) {
                ra.addFlashAttribute("message", "카페 정보가 유실되어 리뷰를 저장하지 못했습니다. 다시 시도해주세요.");
                return "redirect:/";
            }
        }

        // 2) 로그인 사용자 조회
        String email = authentication.getName(); // Jwt나 세션에서 가져온 사용자 식별자
        Member me = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다."));

        // 3) 카페 조회
        Cafe cafe = cafeRepository.findById(form.getCafeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카페입니다."));

        // 4) 엔티티 생성 및 저장
        Review review = form.toEntity(cafe, me); // DTO→엔티티 변환
        if (review.getCreatedAt() == null) review.setCreatedAt(LocalDateTime.now()); // @PrePersist가 있다면 생략 가능
        reviewRepository.save(review);

        // 5) 설문/태그 개별 저장
        if (form.getWaitingTime() != null)
            reviewTagRepository.save(new ReviewTag(null, review, "WAIT", String.valueOf(form.getWaitingTime())));
        if (form.getCompanionType() != null && !form.getCompanionType().isBlank())
            reviewTagRepository.save(new ReviewTag(null, review, "COMPANION", form.getCompanionType()));
        if (form.getTaste() != null)
            reviewTagRepository.save(new ReviewTag(null, review, "TASTE", String.valueOf(form.getTaste())));
        if (form.getLikedTagCodes() != null)
            for (String code : form.getLikedTagCodes())
                reviewTagRepository.save(new ReviewTag(null, review, "LIKE", code));
        if (form.getSentiment() != null && !form.getSentiment().isBlank())
            reviewTagRepository.save(new ReviewTag(null, review, "SENTIMENT", form.getSentiment()));

        // TODO: photos 저장은 기존 업로더 모듈 연결
        ra.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        return "redirect:/cafes/" + form.getCafeId();
    }

    @GetMapping("/cafes/{cafeId}/reviews")
    public String list(@PathVariable Long cafeId, Model model) {
        List<Review> reviews = reviewRepository.findByCafeIdWithMember(cafeId); // fetch join 쿼리 권장
        model.addAttribute("reviews", reviews);
        return "reviews/list";
    }
}