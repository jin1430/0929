// src/main/java/com/example/GoCafe/service/CafeService.java
package com.example.GoCafe.service;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeService {

    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService; // 파일 저장기(로컬/클라우드)

    /* ========================
     * 조회
     * ======================== */
    @Transactional(readOnly = true)
    public List<Cafe> findAll() {
        return cafeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cafe findById(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Cafe> findByStatus(CafeStatus status) {
        return cafeRepository.findByStatus(status);
    }

    /* ========================
     * 생성/수정/삭제 (엔티티 기반)
     * ======================== */
    @Transactional
    public Cafe create(Cafe entity) {
        // 신규 등록은 항상 대기 상태로
        EntityIdUtil.setId(entity, null);
        if (entity.getStatus() == null) entity.setStatus(CafeStatus.PENDING);
        return cafeRepository.save(entity);
    }

    @Transactional
    public Cafe update(Long id, Cafe patch) {
        Cafe c = cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));

        // 소유자/상태/뷰/등록일 등은 보존. 나머지 주요 정보만 갱신
        if (patch.getCafeName() != null)     c.setCafeName(patch.getCafeName());
        if (patch.getCafeAddress() != null)  c.setCafeAddress(patch.getCafeAddress());
        if (patch.getCafeLat() != null)      c.setCafeLat(patch.getCafeLat());
        if (patch.getCafeLon() != null)      c.setCafeLon(patch.getCafeLon());
        if (patch.getCafeNumber() != null)   c.setCafeNumber(patch.getCafeNumber());
        if (patch.getCafePhoto() != null)    c.setCafePhoto(patch.getCafePhoto());
        if (patch.getCafeCode() != null)     c.setCafeCode(patch.getCafeCode());
        // 선택 필드들(없을 수도 있어 try)
        try { if (patch.getBizDoc() != null) c.setBizDoc(patch.getBizDoc()); } catch (Throwable ignored) {}
        return c;
    }

    @Transactional
    public void delete(Long id) {
        if (!cafeRepository.existsById(id)) {
            throw new NotFoundException("Cafe not found: " + id);
        }
        cafeRepository.deleteById(id);
        // 필요하면 파일도 정리(fileStorageService.delete(...)) — 현재는 생략
    }

    /* ========================
     * 생성 (폼 + 파일 업로드)
     * ======================== */

    // 기존 호환 시그니처
    @Transactional
    public Long createCafe(Long cafeOwnerId, CafeForm form) throws AccessDeniedException {
        return createCafe(cafeOwnerId, form, null, null);
    }

    // 폼 + 사진/증빙 파일 업로드 포함
    @Transactional
    public Long createCafe(Long cafeOwnerId,
                           CafeForm form,
                           MultipartFile cafePhotoFile,
                           MultipartFile bizDocFile) throws AccessDeniedException {

        // 1) 소유자 확인
        Member member = memberRepository.findById(cafeOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 중복 방지(선택)
        if (form.getCafeName() != null && cafeRepository.existsByCafeName(form.getCafeName())) {
            throw new IllegalArgumentException("이미 존재하는 카페명입니다.");
        }
        if (form.getCafeNumber() != null && cafeRepository.existsByCafeNumber(form.getCafeNumber())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        // 3) 엔티티 생성(기본 상태 PENDING)
        Cafe cafe = form.toEntity();
        cafe.setStatus(CafeStatus.PENDING);
        // 소유자 지정
        cafe.setCafeOwnerId(cafeOwnerId);

        Cafe saved = cafeRepository.save(cafe);

        // 4) 파일 저장 후 URL 세팅(더티체킹)
        if (cafePhotoFile != null && !cafePhotoFile.isEmpty()) {
            String photoUrl = fileStorageService.save(cafePhotoFile, "cafes/" + saved.getCafeId());
            saved.setCafePhoto(photoUrl);
        }
        if (bizDocFile != null && !bizDocFile.isEmpty()) {
            String docUrl = fileStorageService.save(bizDocFile, "cafes/" + saved.getCafeId() + "/docs");
            try { saved.setBizDoc(docUrl); } catch (Throwable ignored) {}
        }

        // 5) 역할 승격(owner)
        String role = member.getMemberRole();
        if (role == null || !"owner".equalsIgnoreCase(role)) {
            member.setMemberRole("owner");
            memberRepository.save(member);
        }

        return saved.getCafeId();
    }

    /* ========================
     * 단건 필드 업데이트(파일 경로 등)
     * ======================== */
    @Transactional
    public void updateCafePhoto(Long cafeId, String photoUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
        c.setCafePhoto(photoUrl);
    }

    @Transactional
    public void updateCafeBizDoc(Long cafeId, String docUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
        try { c.setBizDoc(docUrl); } catch (Throwable e) {
            throw new IllegalStateException("Cafe 엔티티에 setBizDoc(String)이 없습니다.", e);
        }
    }

    /* ========================
     * 승인/거절
     * ======================== */
    @Transactional
    public void changeStatus(Long cafeId, CafeStatus status) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + cafeId));
        c.setStatus(status);
    }

    @Transactional public void approve(Long cafeId) { changeStatus(cafeId, CafeStatus.APPROVED); }
    @Transactional public void reject(Long cafeId)  { changeStatus(cafeId, CafeStatus.REJECTED); }
}
