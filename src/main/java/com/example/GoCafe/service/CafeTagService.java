package com.example.GoCafe.service;

import com.example.GoCafe.entity.CafeTag;
import com.example.GoCafe.repository.CafeTagRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeTagService {

    private final CafeTagRepository repository;

    @Transactional(readOnly = true)
    public List<CafeTag> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public CafeTag findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("CafeTag not found: " + id));
    }

    @Transactional
    public CafeTag create(CafeTag entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public CafeTag update(Long id, CafeTag entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("CafeTag not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("CafeTag not found: " + id);
        }
        repository.deleteById(id);
    }
}
