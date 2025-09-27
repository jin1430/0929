package com.example.GoCafe.service;

import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.repository.ReviewTagRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewTagService {

    private final ReviewTagRepository repository;

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
}
