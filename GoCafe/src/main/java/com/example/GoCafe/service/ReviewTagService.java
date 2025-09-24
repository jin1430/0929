package com.example.GoCafe.service;

import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.event.ReviewChangedEvent;
import com.example.GoCafe.repository.ReviewTagRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewTagService {

    private final ReviewTagRepository repository;
    private final ApplicationEventPublisher publisher;


    @Transactional(readOnly = true)
    public List<ReviewTag> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public ReviewTag findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("ReviewTag not found: " + id));
    }

    @Transactional
    public ReviewTag create(ReviewTag entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public ReviewTag update(Long id, ReviewTag entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("ReviewTag not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("ReviewTag not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void upsertTags(Long reviewId, List<ReviewTag> tags) {
        // 기존 태그 정리 및 저장 로직
        // reviewTagRepo.deleteByReviewId(reviewId) 등 네가 쓰는 방식 유지
        repository.saveAll(tags);  // score 포함 저장
        Long cafeId = tags.isEmpty() ? null : tags.get(0).getReview().getCafe().getId();
        if (cafeId != null) publisher.publishEvent(new ReviewChangedEvent(cafeId));
    }

}
