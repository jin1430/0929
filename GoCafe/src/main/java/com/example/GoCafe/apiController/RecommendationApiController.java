package com.example.GoCafe.apiController;

import com.example.GoCafe.dto.RecommendedCafeDto;
import com.example.GoCafe.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 인증은 프로젝트의 기존 Security 설정을 그대로 사용.
 * 로그인 사용자는 Authentication을 통해 memberId를 구해 전달하면 됨.
 */
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationApiController {

    private final RecommendationService service;

    /** 내 취향 기반 추천 */
    @GetMapping
    public ResponseEntity<List<RecommendedCafeDto>> recommend(Authentication auth,
                                                              @RequestParam(defaultValue = "20") int limit) {
        Long memberId = extractMemberId(auth); // 프로젝트의 방식에 맞춰 구현
        List<RecommendedCafeDto> result = service.recommendForUser(memberId, Math.max(1, Math.min(100, limit)));
        return ResponseEntity.ok(result);
    }

    /** 특정 카페와 유사한 곳 */
    @GetMapping("/similar/{cafeId}")
    public ResponseEntity<List<RecommendedCafeDto>> similar(@PathVariable Long cafeId,
                                                            @RequestParam(defaultValue = "12") int limit) {
        List<RecommendedCafeDto> result = service.similarCafes(cafeId, Math.max(1, Math.min(100, limit)));
        return ResponseEntity.ok(result);
    }

    // === 프로젝트의 인증/멤버 추출 방식에 맞춰 교체 ===
    private Long extractMemberId(Authentication auth) {
        if (auth == null) return null;
        // 예시) Principal에서 이메일 -> Member 조회해서 id 가져오기
        // 현재 프로젝트에 이미 MemberRepository 또는 SecurityUtils가 있을 가능성이 큼.
        // 우선 null 리턴하여 비로그인/콜드스타트 경로도 동작하게 함.
        return null;
    }
}
