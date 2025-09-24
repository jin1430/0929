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
import com.example.GoCafe.service.NotificationService;
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
    private final NotificationService notificationService;


    // 폴백 페이지(팝업이 안 뜰 때만 노출). view name은 절대 슬래시 X
    @GetMapping("/new")
    public String newForm(@RequestParam("cafeId") Long cafeId) {
        // 브라우저 주소: /cafes/{id}#reviewModal  => CSS :target로 즉시 오픈
        return "redirect:/cafes/" + cafeId + "#reviewModal";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute ReviewForm form,
                         @RequestParam(value = "photos", required = false) MultipartFile[] photos,
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
            String fromMain = request.getParameter("cafeId");
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
        String email = authentication.getName();
        Member me = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다."));

        // 3) 카페 조회
        Cafe cafe = cafeRepository.findById(form.getCafeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카페입니다."));

        // 4) 엔티티 생성
        Review review = form.toEntity(cafe, me);
        if (review.getCreatedAt() == null) review.setCreatedAt(LocalDateTime.now());

        // 4-1) sentiment → good/bad 치환 (컨트롤러에서 처리)
        // form 우선, 없으면 raw 파라미터도 보조로 확인
        String s = (form.getSentiment() != null) ? form.getSentiment() : request.getParameter("sentiment");
        if (s != null) s = s.trim().toUpperCase();
        // 저장되는 문자열은 GOOD/BAD만 허용
        if (!"GOOD".equals(s) && !"BAD".equals(s)) s = null;
        review.setSentiment(s);

        // 신규 저장 시 초기값 세팅(이미 값이 들어있다면 덮어쓰지 않음)
        if (review.getGood() == 0 && review.getBad() == 0) {
            if ("GOOD".equals(s)) { review.setGood(1); review.setBad(0); }
            else if ("BAD".equals(s)) { review.setGood(0); review.setBad(1); }
        }

        // 4-2) 저장
        reviewRepository.save(review);


        if (cafe.getOwner() != null &&
                (review.getMember() == null ||
                        !cafe.getOwner().getId().equals(review.getMember().getId()))) {
            notificationService.notifyReviewCreated(review);
        }
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
        // sentiment 태그도 정규화된 s로 저장(폼 원본 대신)
        if (s != null)
            reviewTagRepository.save(new ReviewTag(null, review, "SENTIMENT", s));

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
    // 전체 리뷰 목록 (페이징)
    @GetMapping
    public String allReviews(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "12") int size,
                             Model model) {
        if (page < 1) page = 1;
        if (size < 1) size = 12;

        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        var pageData = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);

        // Mustache에서 안전 사용을 위해 뷰 맵으로 변환
        java.util.List<java.util.Map<String,Object>> items = new java.util.ArrayList<>();
        for (Review r : pageData.getContent()) {
            java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("cafeId", r.getCafe() != null ? r.getCafe().getId() : null);
            m.put("cafeName", r.getCafe() != null ? r.getCafe().getName() : "(알 수 없음)");
            m.put("memberNickname", r.getMemberNickname());      // Review 엔티티에 @Transient로 제공됨
            m.put("createdAt", r.getCreatedAtFmt());             // Review 엔티티에 @Transient로 제공됨
            // 본문 요약 120자
            String content = null;
            try { content = (String) Review.class.getMethod("getContent").invoke(r); } catch (Exception ignored) {}
            if (content == null) content = "";
            String excerpt = content.length() > 120 ? content.substring(0, 117) + "..." : content;
            m.put("excerpt", excerpt);
            // 평점/감정(있으면)
            try { Object rating = Review.class.getMethod("getRating").invoke(r); if (rating != null) m.put("rating", rating); } catch (Exception ignored) {}
            try { Object s = Review.class.getMethod("getSentiment").invoke(r); if (s != null) m.put("sentiment", String.valueOf(s)); } catch (Exception ignored) {}
            // 좋아요/싫어요 카운트(있으면)
            try { Object g = Review.class.getMethod("getGood").invoke(r); if (g != null) m.put("good", g); } catch (Exception ignored) {}
            try { Object b = Review.class.getMethod("getBad").invoke(r); if (b != null) m.put("bad", b); } catch (Exception ignored) {}

            items.add(m);
        }

        model.addAttribute("reviews", items);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalElements", pageData.getTotalElements());
        model.addAttribute("hasPrev", page > 1);
        model.addAttribute("hasNext", page < Math.max(1, pageData.getTotalPages()));
        model.addAttribute("prevPage", Math.max(1, page - 1));
        model.addAttribute("nextPage", Math.min(Math.max(1, pageData.getTotalPages()), page + 1));

        return "reviews/reviews"; // 아래 템플릿
    }

    // --- 투표 (좋아요/싫어요) ---
    @PostMapping("/{id}/good")
    public String voteGood(@PathVariable Long id, HttpServletRequest req) {
                Review r = reviewRepository.findById(id).orElseThrow();
                Integer g = r.getGood(); if (g == null) g = 0; r.setGood(g + 1);
                reviewRepository.save(r);
                String ref = req.getHeader("Referer");
                return (ref != null ? "redirect:" + ref : "redirect:/reviews");
            }

    @PostMapping("/{id}/bad")
    public String voteBad(@PathVariable Long id, HttpServletRequest req) {
                Review r = reviewRepository.findById(id).orElseThrow();
                Integer b = r.getBad(); if (b == null) b = 0; r.setBad(b + 1);
                reviewRepository.save(r);
                String ref = req.getHeader("Referer");
                return (ref != null ? "redirect:" + ref : "redirect:/reviews");
            }
}