package com.example.GoCafe.service;

import com.example.GoCafe.dto.NeedSelection;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.UserNeeds;
import com.example.GoCafe.repository.UserNeedsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNeedsService {

    private final UserNeedsRepository userNeedsRepository;

    /** 기존 선택을 모두 삭제하고, 전달된 DTO들로 전부 weight=1.0 저장 */
    @Transactional
    public void replaceSelections(Long memberId, List<NeedSelection> selections) {
        // 1) 기존 삭제
        userNeedsRepository.deleteByMember_Id(memberId);

        if (selections == null || selections.isEmpty()) {
            return;
        }

        // 2) Member 프록시 생성 (쿼리 줄이기용)
        Member ref = new Member();
        ref.setId(memberId);

        // 3) 전부 저장 (태그 '이름'을 UserNeeds.code에 저장)
        for (NeedSelection s : selections) {
            UserNeeds e = new UserNeeds();
            e.setMember(ref);
            e.setCategoryCode(s.getCategoryCode());
            e.setCode(s.getTagName());   // ← 폼에서 넘어온 건 태그 "이름"이므로 code 필드에 저장
            e.setWeight(1.0);            // 항상 1.0
            userNeedsRepository.save(e);
        }
    }

    @Transactional
    public void clearForMember(Long memberId) {
        userNeedsRepository.deleteByMember_Id(memberId);
    }

    @Transactional(readOnly = true)
    public List<NeedSelection> listSelections(Long memberId) {
        return userNeedsRepository.findByMember_IdOrderByCategoryCodeAscCodeAsc(memberId)
                .stream()
                .map(u -> new NeedSelection(
                        u.getCategoryCode(), // DTO.categoryCode
                        u.getCode(),         // ✅ DTO.tagName <= 엔티티 code
                        u.getWeight()        // DTO.weight
                ))
                .toList();
    }
}
