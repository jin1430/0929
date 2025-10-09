// src/main/java/com/example/GoCafe/event/ReviewChangedListener.java
package com.example.GoCafe.event;

import com.example.GoCafe.service.TagAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewChangedListener {
    private final TagAggregationService svc;

    @EventListener
    public void onReviewChanged(ReviewChangedEvent ev) {
        svc.recomputeForCafe(ev.cafeId());
    }
}
