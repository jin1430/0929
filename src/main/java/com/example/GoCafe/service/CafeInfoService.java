package com.example.GoCafe.service;

import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.repository.CafeInfoRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CafeInfoService {

    private final CafeInfoRepository repository;

    @Transactional(readOnly = true)
    public List<CafeInfo> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public CafeInfo findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("CafeInfo not found: " + id));
    }

    @Transactional
    public CafeInfo create(CafeInfo entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public CafeInfo update(Long id, CafeInfo entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("CafeInfo not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("CafeInfo not found: " + id);
        }
        repository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public Optional<CafeInfo> findByCafeId(Long cafeId) {
        return repository.findByCafe_Id(cafeId);
    }

    @Transactional
    public CafeInfo upsertByCafeId(Long cafeId, CafeInfo entity) {
        Optional<CafeInfo> cur = repository.findByCafe_Id(cafeId);
        if (cur.isPresent()) {
            // 기존 행이 있으면 id를 복사해 update로 전환
            com.example.GoCafe.support.EntityIdUtil.setId(entity,
                    com.example.GoCafe.support.EntityIdUtil.getId(cur.get()));
        }
        return repository.save(entity);
    }


}
