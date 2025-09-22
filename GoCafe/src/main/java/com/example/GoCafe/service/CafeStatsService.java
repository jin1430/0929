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
        // This part is fine
        int good = reviewRepository.countByCafe_IdAndSentiment(cafeId, "GOOD");
        int bad = reviewRepository.countByCafe_IdAndSentiment(cafeId, "BAD");

        List<Map<String, Object>> tags = new ArrayList<>();

        // ✅ This is the corrected loop
        for (ReviewTagRepository.TagCount tagCount : reviewTagRepository.findLikeTagCountsGood(cafeId)) {
            // Use getter methods instead of array indices
            String code = tagCount.getCode();
            long cnt = tagCount.getCnt();

            tags.add(Map.of("code", code, "cnt", cnt));
            if (tags.size() >= topN) {
                break;
            }
        }

        return Map.of("good", good, "bad", bad, "tags", tags);

    }
}
