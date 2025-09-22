// src/main/java/com/example/GoCafe/service/ReviewService.java
package com.example.GoCafe.service;

import com.example.GoCafe.dto.MyReviewItem;
import com.example.GoCafe.dto.ReviewCreateForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.repository.*;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final NotificationService notificationService;
    private final ReviewTagRepository reviewTagRepository;
    private final NotificationRepository notificationRepository;
    private final ProGateService proGateService;
    private final CafeInfoRepository cafeInfoRepository;
    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found: " + id));
    }

    @Transactional
    public Review create(ReviewCreateForm form, Long memberId) {
        // 1) 기존 저장 로직
        Review saved = reviewRepository.save(mapToEntity(form, memberId));

        // 2) 미션 완료 처리 (수락 기록이 있으면)
        notificationRepository.latestMissionLog(memberId, form.getCafeId()).ifPresent(n -> {
            if (!n.getMessage().startsWith("MISSION:COMPLETED")) {
                n.setMessage("MISSION:COMPLETED:" + saved.getId());
                n.setRead(true);
                notificationRepository.save(n);
            }
        });

        // 3) 협찬 태그 자동 보강 (해당 카페 공지가 [협찬]이면)
        String notice = cafeInfoRepository.findByCafe_Id(form.getCafeId()).map(CafeInfo::getNotice).orElse("");
        if (notice != null && notice.contains("[협찬]")) {
            addDisclosureTags(saved.getId()); // 아래 헬퍼
        }

        proGateService.refreshRoleKind(memberId);
        try {
            notificationService.notifyReviewCreated(saved);
        } catch (Exception ignore) {
        }
        return saved;
    }


    @Transactional
    public Review update(Long id, Review entity) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return reviewRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    // 카페 상세에서 사용할 리뷰 목록(최신순)
    @Transactional(readOnly = true)
    public List<Review> findByCafeIdWithMember(Long cafeId) {
        return reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId);
    }

    // 홈의 최근 올라온 후기
    @Transactional(readOnly = true)
    public List<Review> findRecentTop10() {
        return reviewRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public Page<MyReviewItem> findMyReviews(Long memberId, Pageable pageable) {
        return reviewRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toItem);
    }

    private MyReviewItem toItem(Review review) {
        Cafe cafe = review.getCafe();
        Long cafeId = (cafe != null ? cafe.getId() : null);
        String cafeName = (cafe != null ? cafe.getName() : "(알 수 없음)");

        // 카페 사진 url
        String cafeMainPhotoUrl = String.valueOf(cafePhotoRepository.findMainPhoto(cafe.getId()).get());
        try {
            // 예시: c.getCafeThumb() 가 있으면 /images/cafe/{file} 로 만든다
            var method = Cafe.class.getMethod("getCafeThumb");
            Object v = (cafe != null ? method.invoke(cafe) : null);
            if (v != null) {
                String s = String.valueOf(v);
                if (!s.isBlank()) cafeMainPhotoUrl = "/images/cafe/" + s;
            }
        } catch (Exception ignore) {
            // 필드가 없으면 null -> 템플릿에서 NO IMG
        }

        return new MyReviewItem(
                review.getId(),
                cafeId,
                cafeName,
                cafeMainPhotoUrl,
                safe(review.getContent()),
                review.getCreatedAt(),
                review.getGood(),
                review.getBad()
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private void addDisclosureTags(Long reviewId) {
        List<String> need = List.of("협찬", "광고");
        for (String code : need) {
            if (!reviewTagRepository.existsById(reviewId)) {
                ReviewTag tag = new ReviewTag();
                tag.setReview(reviewRepository.getReferenceById(reviewId));
                tag.setCategoryCode("DISCLOSURE");
                tag.setCode(code);
                reviewTagRepository.save(tag);
            }
        }

    }
    private Review mapToEntity(ReviewCreateForm form, Long memberId) {
        Review r = new Review();
        r.setCafe(cafeRepository.getReferenceById(form.getCafeId()));
        r.setMember(memberRepository.getReferenceById(memberId));

        // content(또는 text/reviewContent 등) 세팅
        String content = null;
        try { content = (String) ReviewCreateForm.class.getMethod("getReviewContent").invoke(form); } catch (Exception ignored) {}
        if (content == null) {
            try { content = (String) ReviewCreateForm.class.getMethod("getContent").invoke(form); } catch (Exception ignored) {}
        }
        if (content == null) content = "";
        try { Review.class.getMethod("setContent", String.class).invoke(r, content); } catch (Exception e) { /* 엔티티 필드명이 다르면 알려줘 */ }

        // rating 있으면 세팅(선택 필드)
        Integer rating = null;
        try { Object v = ReviewCreateForm.class.getMethod("getRating").invoke(form); if (v != null) rating = (Integer) v; } catch (Exception ignored) {}
        if (rating != null) {
            try { Review.class.getMethod("setRating", Integer.class).invoke(r, rating); } catch (Exception ignored) {}
            try { Review.class.getMethod("setRating", int.class).invoke(r, rating); } catch (Exception ignored) {}
        }

        // sentiment 기본값 GOOD
        String sentiment = "GOOD";
        try {
            Object v = ReviewCreateForm.class.getMethod("getSentiment").invoke(form);
            if (v != null) sentiment = String.valueOf(v);
        } catch (Exception ignored) {}
        try { Review.class.getMethod("setSentiment", String.class).invoke(r, sentiment); } catch (Exception ignored) {}

        return r;
    }

    private String extractTagCode(ReviewTag t) {
        try { Object v = ReviewTag.class.getMethod("getTagCode").invoke(t); if (v!=null) return v.toString(); } catch (Exception ignored) {}
        try { Object v = ReviewTag.class.getMethod("getCode").invoke(t);    if (v!=null) return v.toString(); } catch (Exception ignored) {}
        try { Object v = ReviewTag.class.getMethod("getTag").invoke(t);     if (v!=null) return v.toString(); } catch (Exception ignored) {}
        return null;
    }

    /** ReviewTag에 '코드' 값을 안전하게 넣기 (setTagCode/setCode/setTag 중 있는 것 호출) */
    private void setTagCodeSafely(ReviewTag t, String code) {
        try { ReviewTag.class.getMethod("setTagCode", String.class).invoke(t, code); return; } catch (Exception ignored) {}
        try { ReviewTag.class.getMethod("setCode", String.class).invoke(t, code);    return; } catch (Exception ignored) {}
        try { ReviewTag.class.getMethod("setTag", String.class).invoke(t, code);     return; } catch (Exception ignored) {}
        // 위 세터가 아무것도 없다면, 프로젝트의 실제 필드명을 알려줘. 그에 맞게 한 줄만 바꿔 줄게.
    }
}
