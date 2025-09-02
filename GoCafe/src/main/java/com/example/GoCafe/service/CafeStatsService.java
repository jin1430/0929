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
        int good = reviewRepository.countByCafe_CafeIdAndSentiment(cafeId, "GOOD");
        int bad  = reviewRepository.countByCafe_CafeIdAndSentiment(cafeId, "BAD");

        List<Map<String, Object>> tags = new ArrayList<>();
        for (Object[] row : reviewTagRepository.findLikeTagCountsGood(cafeId)) {
            String tag = (String) row[0];
            long cnt   = ((Number) row[1]).longValue();
            tags.add(Map.of("tagCategoryCode", "LIKE", "tagCode", tag, "count", cnt));
            if (tags.size() >= topN) break;
        }
        return Map.of("good", good, "bad", bad, "tags", tags);
    }
}
