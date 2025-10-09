// src/main/java/com/example/GoCafe/bootstrap/TagRecomputeRunner.java
package com.example.GoCafe.bootstrap;

import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.service.TagAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
//@Profile("dev")
@RequiredArgsConstructor
public class TagRecomputeRunner {

    private final ReviewRepository reviewRepository;
    private final TagAggregationService tagAggregationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // data.sql 로드가 끝난 뒤에 호출됨 (application.properties의
        // spring.sql.init.mode=always + spring.jpa.defer-datasource-initialization=true 가정)
        List<Long> cafeIds = reviewRepository.findCafeIdsHavingReviews();
        if (cafeIds.isEmpty()) {
            log.info("[TagRecomputeRunner] 리뷰가 있는 카페가 없어 초기 재집계를 건너뜁니다.");
            return;
        }
        log.info("[TagRecomputeRunner] 초기 재집계 시작: 대상 카페 수={}", cafeIds.size());
        for (Long cafeId : cafeIds) {
            try {
                tagAggregationService.recomputeForCafe(cafeId);
                log.info("[TagRecomputeRunner] 재집계 완료 cafeId={}", cafeId);
            } catch (Exception e) {
                log.warn("[TagRecomputeRunner] 재집계 실패 cafeId={}", cafeId, e);
            }
        }
        log.info("[TagRecomputeRunner] 초기 재집계 완료");
    }
}
