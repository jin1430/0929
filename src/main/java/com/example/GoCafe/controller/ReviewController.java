package com.example.GoCafe.controller;

import com.example.GoCafe.dto.ReviewForm;
import com.example.GoCafe.dto.ReviewTagForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.service.*;
import com.example.GoCafe.support.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final ReviewTagService reviewTagService;
    private final ReviewPhotoService reviewPhotoService;
    private final CafeService cafeService;
    private final MemberService memberService;
    private final TagAggregationService tagAggregationService;

    // 폴백 페이지(팝업이 안 뜰 때만 노출). view name은 절대 슬래시 X
    @GetMapping("/new")
    public String newForm(@RequestParam("cafeId") Long cafeId) {
        // 브라우저 주소: /cafes/{id}#reviewModal  => CSS :target로 즉시 오픈
        return "redirect:/cafes/" + cafeId + "#reviewModal";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createReview(Authentication auth,
                               @ModelAttribute ReviewForm form, // hidden cafeId 포함
                               @RequestParam(value = "tags", required = false) List<String> tagParams, // ← 추가
                               @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                               RedirectAttributes ra) {

        // 0) cafeId 확보 (hidden 필수)
        Long cafeId = form.getCafeId();
        if (cafeId == null) {
            ra.addFlashAttribute("error", "카페 정보가 없습니다.");
            return "redirect:/";
        }

        // 1) 엔티티 로드 + 인증 확인
        Cafe cafe = cafeService.findById(cafeId);
        if (cafe == null) throw new NotFoundException("카페가 없습니다.");

        Member me = memberService.findByEmail(auth.getName());
        if (me == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        // 2) 검증 (평점/내용)  — taste 병합 로직 삭제, rating만 사용
        Integer rating = form.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "평점은 1~5 사이여야 합니다.");
            return "redirect:/cafes/" + cafeId + "#reviews";
        }
        if (form.getReviewContent() == null || form.getReviewContent().trim().length() < 5) {
            ra.addFlashAttribute("error", "리뷰 내용을 5자 이상 입력하세요.");
            return "redirect:/cafes/" + cafeId + "#reviews";
        }

        // 3) 저장
        form.setCafeId(cafeId);
        Review review = form.toEntity(cafe, me);
        reviewService.save(review);

        try {
            reviewTagService.extractAndSaveMenuTags(review, cafeId);
        }
        catch (Exception e) {
            log.info("fastapi refused");
        }

        // 3-1) 선택된 리뷰 태그 파싱 후 전달
        List<ReviewTagForm> pairs = (tagParams == null) ? List.of()
                : tagParams.stream()                         // "CODE|NAME"
                .map(s -> {
                    int i = (s != null) ? s.indexOf('|') : -1;
                    if (i <= 0 || i == s.length() - 1) return null;
                    String code = s.substring(0, i).trim();
                    String name = s.substring(i + 1).trim();
                    if (code.isEmpty() || name.isEmpty()) return null;
                    return new ReviewTagForm(code, name);
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        reviewTagService.saveReviewTags(review, pairs); // ✅ 리뷰태그만 저장

        tagAggregationService.recomputeForCafe(cafeId);

        // 4) 사진(최대 6장 권장)
        if (photos != null && !photos.isEmpty()) {
            if (photos.size() > 6) {
                ra.addFlashAttribute("error", "사진은 최대 6장까지 업로드 가능합니다.");
                return "redirect:/cafes/" + cafeId + "#reviews";
            }
            reviewPhotoService.uploadAndAttachAll(review.getId(), photos);
        }

        ra.addFlashAttribute("msg", "리뷰가 등록되었습니다.");
        return "redirect:/cafes/" + cafeId + "#reviews";
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