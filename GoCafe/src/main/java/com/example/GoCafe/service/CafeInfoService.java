package com.example.GoCafe.service;

import com.example.GoCafe.dto.CafeInfoForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.repository.CafeInfoRepository;
import com.example.GoCafe.repository.CafeRepository;
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
    private final CafeRepository cafeRepository;

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
    @Transactional
    public void createInfo(Long cafeId, CafeInfoForm dto) {
        // 1. Cafe 엔티티를 Service에서 직접 조회
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페를 찾을 수 없습니다. ID: " + cafeId));

        // 2. 새로 추가한 toEntity(cafe) 메서드를 호출
        CafeInfo cafeInfo = dto.toEntity(cafe);

        // 3. 엔티티 저장
        repository.save(cafeInfo);
    }

    @Transactional
    public void updateInfo(Long cafeId, CafeInfoForm dto) {
        CafeInfo cafeInfo = repository.findByCafe_Id(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페 정보를 찾을 수 없습니다. Cafe ID: " + cafeId));

        // 엔티티에 업데이트 로직이 있다면 그대로 사용
        cafeInfo.update(dto);
    }

}
