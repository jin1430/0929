package com.example.GoCafe.service;

import com.example.GoCafe.repository.ReviewRepository;
import com.example.GoCafe.repository.ReviewTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CafeStatsService {

    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;

    public Map<String, Object> buildStats(Long cafeId, int topN) {
        int good = reviewRepository.countByCafe_IdAndSentiment(cafeId, "GOOD");
        int bad  = reviewRepository.countByCafe_IdAndSentiment(cafeId, "BAD");

        List<Map<String, Object>> tags = new ArrayList<>();
        for (Object[] row : reviewTagRepository.findLikeTagCountsGood(cafeId)) {
            String code = (String) row[0];                 // 태그 코드 (예: "맛있어요")
            long cnt    = ((Number) row[1]).longValue();   // 개수
            // 🔧 템플릿이 기대하는 key 이름으로 맞춤
            tags.add(Map.of("code", code, "cnt", cnt));    // ← 여기!
            if (tags.size() >= topN) break;
        }
        return Map.of("good", good, "bad", bad, "tags", tags);
    }

}
