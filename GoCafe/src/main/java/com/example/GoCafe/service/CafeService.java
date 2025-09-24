// src/main/java/com/example/GoCafe/service/CafeService.java
package com.example.GoCafe.service;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.domain.RoleKind;
import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CafeService {

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    /* =========================
     *            READ
     * ========================= */

    @Transactional(readOnly = true)
    public List<Cafe> findAll() {
        return cafeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cafe> findTop8ByViews() {
        return cafeRepository.findTop8ByOrderByViewsDesc();
    }

    @Transactional(readOnly = true)
    public Cafe findById(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Cafe> findByStatus(CafeStatus status) {
        return cafeRepository.findByCafeStatus(status);
    }

    /* =========================
     *           CREATE
     * ========================= */

    @Transactional
    public Cafe create(Cafe entity) {
        EntityIdUtil.setId(entity, null);

        if (entity.getCafeStatus() == null) {
            entity.setCafeStatus(CafeStatus.PENDING);
        }
        if (entity.getCreationDate() == null) {
            entity.setCreationDate(LocalDate.now());
        }
        if (entity.getViews() == null) {
            entity.setViews(0L);
        }
        return cafeRepository.save(entity);
    }

    @Transactional
    public Long createCafe(Long cafeOwnerId, CafeForm form) {
        return createCafe(cafeOwnerId, form, null, null);
    }

    @Transactional
    public Long createCafe(Long cafeOwnerId,
                           CafeForm form,
                           MultipartFile cafePhotoFile,
                           MultipartFile bizDocFile) throws AccessDeniedException {

        // 1) 소유자 확인
        Member cafeOwner = memberRepository.findById(cafeOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 중복 방지
        if (form.getName() != null && cafeRepository.existsByName(form.getName())) {
            throw new IllegalArgumentException("이미 존재하는 카페명입니다.");
        }
        if (form.getPhoneNumber() != null && cafeRepository.existsByPhoneNumber(form.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        // 3) 엔티티 생성 + 기본값 세팅
        Cafe cafe = form.toEntity();
        cafe.setOwner(cafeOwner);
        cafe.setCafeStatus(CafeStatus.PENDING);
        if (cafe.getCreationDate() == null) cafe.setCreationDate(LocalDate.now());
        if (cafe.getViews() == null) cafe.setViews(0L);

        Cafe saved = cafeRepository.save(cafe);

        // 4) 카페 대표 사진 업로드 → CafePhoto INSERT
        if (cafePhotoFile != null && !cafePhotoFile.isEmpty()) {
            String photoUrl = fileStorageService.save(cafePhotoFile, "cafes/" + saved.getId());

            boolean hasMain = cafePhotoRepository.existsByCafe_IdAndIsMainTrue(saved.getId());
            int nextOrder = (int) cafePhotoRepository.countByCafe_Id(saved.getId());

            CafePhoto photo = new CafePhoto();
            photo.setCafe(saved);
            photo.setUrl(photoUrl);
            photo.setSortIndex(nextOrder);
            photo.setIsMain(!hasMain); // 첫 업로드면 자동 메인

            cafePhotoRepository.save(photo);
        }

        // 5) 사업자 증빙 파일 업로드(선택)
        if (bizDocFile != null && !bizDocFile.isEmpty()) {
            String docUrl = fileStorageService.save(bizDocFile, "cafes/" + saved.getId() + "/docs");
            try {
                saved.setBizDoc(docUrl);
            } catch (Throwable e) {
                throw new IllegalStateException("Cafe 엔티티에 setBizDoc(String)이 없습니다.", e);
            }
        }

        // 6) 역할 승격 로직 (버그 수정)
        //    - 기존 코드는 ADMIN/OWNER도 무조건 OWNER로 바꿔 '강등'되는 문제가 있었음.
        //    - 이제 MEMBER 또는 PRO 인 경우에만 OWNER로 승격함. ADMIN은 그대로 유지.
        RoleKind currentRole;
        Object roleObj = cafeOwner.getRoleKind();
        if (roleObj instanceof RoleKind enumVal) {
            currentRole = enumVal;
        } else {
            currentRole = RoleKind.from(roleObj == null ? null : String.valueOf(roleObj));
        }

        if (currentRole == RoleKind.MEMBER || currentRole == RoleKind.PRO) {
            try {
                cafeOwner.setRoleKind(RoleKind.OWNER);
            } catch (Throwable ignored) {
                try {
                    cafeOwner.getClass().getMethod("setRoleKind", String.class)
                            .invoke(cafeOwner, "OWNER");
                } catch (Throwable e) {
                    throw new IllegalStateException("Member.setRoleKind(RoleKind|String) 둘 다 없음", e);
                }
            }
            memberRepository.save(cafeOwner);
        }
        // ADMIN, OWNER는 그대로 유지

        return saved.getId();
    }

    /* =========================
     *           UPDATE
     * ========================= */

    @Transactional
    public Cafe update(Long id, Cafe patch) {
        Cafe c = cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));

        if (patch.getName() != null) c.setName(patch.getName());
        if (patch.getAddress() != null) c.setAddress(patch.getAddress());
        if (patch.getLat() != null) c.setLat(patch.getLat());
        if (patch.getLon() != null) c.setLon(patch.getLon());

        if (patch.getPhoneNumber() != null) {
            boolean dup = cafeRepository.existsByPhoneNumber(patch.getPhoneNumber())
                    && !Objects.equals(c.getPhoneNumber(), patch.getPhoneNumber());
            if (dup) throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
            c.setPhoneNumber(patch.getPhoneNumber());
        }

        if (patch.getBusinessCode() != null) c.setBusinessCode(patch.getBusinessCode());
        if (patch.getCreationDate() != null) c.setCreationDate(patch.getCreationDate());

        try {
            if (patch.getBizDoc() != null) c.setBizDoc(patch.getBizDoc());
        } catch (Throwable ignored) {}

        return c;
    }

    @Transactional
    public void updateCafePhoto(Long cafeId, String photoUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));

        List<CafePhoto> all = cafePhotoRepository.findByCafe_Id(cafeId);
        for (CafePhoto p : all) p.setIsMain(false);

        CafePhoto photo = new CafePhoto();
        photo.setCafe(c);
        photo.setUrl(photoUrl);
        photo.setIsMain(true);
        photo.setSortIndex(all.size());

        cafePhotoRepository.save(photo);
    }

    @Transactional
    public void updateCafeBizDoc(Long cafeId, String docUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
        try {
            c.setBizDoc(docUrl);
        } catch (Throwable e) {
            throw new IllegalStateException("Cafe 엔티티에 setBizDoc(String)이 없습니다.", e);
        }
    }

    /* =========================
     *           DELETE
     * ========================= */

    @Transactional
    public void delete(Long id) {
        if (!cafeRepository.existsById(id)) {
            throw new NotFoundException("Cafe not found: " + id);
        }
        cafeRepository.deleteById(id);
        // 필요 시 파일 스토리지 정리: fileStorageService.delete("cafes/" + id);
    }

    /* =========================
     *        STATUS / SEARCH
     * ========================= */

    // ── 추가: 관리자 컨트롤러가 호출하는 메서드
    @Transactional
    public void updateStatus(Long cafeId, CafeStatus status) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + cafeId));
        c.setCafeStatus(status);
        try {
            notificationService.notifyCafeStatus(c, status);
        } catch (Exception ignore) { }
    }

    @Transactional
    public void changeStatus(Long cafeId, CafeStatus status) {
        updateStatus(cafeId, status);
    }

    @Transactional
    public void approve(Long cafeId) { updateStatus(cafeId, CafeStatus.APPROVED); }

    @Transactional
    public void reject(Long cafeId) { updateStatus(cafeId, CafeStatus.REJECTED); }

    @Transactional(readOnly = true)
    public List<Cafe> findApprovedTopByViews(int limit) {
        return cafeRepository.findByCafeStatusOrderByViewsDesc(
                CafeStatus.APPROVED, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Cafe getOrThrow(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카페가 없습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public List<Cafe> searchApproved(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return cafeRepository.findByCafeStatusOrderByViewsDesc(
                    CafeStatus.APPROVED, PageRequest.of(0, 40));
        }
        return cafeRepository.findByCafeStatusAndNameContainingOrCafeStatusAndAddressContaining(
                CafeStatus.APPROVED, keyword, CafeStatus.APPROVED, keyword);
    }

    @Transactional(readOnly = true)
    public long countByStatus(CafeStatus status) {
        return cafeRepository.countByCafeStatus(status);
    }

    @Transactional(readOnly = true)
    public long countAll() { return cafeRepository.count(); }

    // ===== (추가) 가시성 로직: 승인된 것 + (내가 소유한) 대기중 포함, 관리자면 전체 =====
    @Transactional(readOnly = true)
    public List<Cafe> searchVisible(String keyword, Long viewerMemberId, boolean isAdmin) {
                if (isAdmin) {
                        // 관리자: 키워드 없으면 전체, 있으면 전체 상태에서 키워드 매칭
                                if (keyword == null || keyword.isBlank()) return cafeRepository.findAll();
                        return cafeRepository.findByNameContainingOrAddressContaining(keyword, keyword);
                    }

                        // 기본: 승인된 결과
                                List<Cafe> approved = searchApproved(keyword);

                        if (viewerMemberId == null) return approved;

                        // 소유자: 본인 소유의 PENDING도 보이게
                                List<Cafe> minePending;
                if (keyword == null || keyword.isBlank()) {
                        minePending = cafeRepository.findByOwner_IdAndCafeStatus(viewerMemberId, CafeStatus.PENDING);
                    } else {
                        minePending = cafeRepository.findByOwner_IdAndCafeStatusAndNameContainingOrOwner_IdAndCafeStatusAndAddressContaining(
                                        viewerMemberId, CafeStatus.PENDING, keyword,
                                        viewerMemberId, CafeStatus.PENDING, keyword
                                        );
                    }
                // distinct merge by id
                        java.util.Map<Long, Cafe> map = new java.util.LinkedHashMap<>();
                for (Cafe c : approved) map.put(c.getId(), c);
                for (Cafe c : minePending) map.put(c.getId(), c);
                return new java.util.ArrayList<>(map.values());
            }

    /* =========================
     *         FILE ACCESS
     * ========================= */

    // ── 추가: 증빙서 파일 바이트 로드(관리자 다운로드용)
    @Transactional(readOnly = true)
    public byte[] loadBizDocBytes(Long cafeId) {
        Cafe cafe = findById(cafeId);
        String path = cafe.getBizDoc(); // 저장 시 넣어둔 경로/키
        if (path == null || path.isBlank()) {
            throw new NotFoundException("증빙서가 없습니다. cafeId=" + cafeId);
        }
        // FileStorageService에 맞는 로더 사용 (예: loadAsBytes, read, getBytes 등 네 구현에 맞춰 호출)
        return fileStorageService.loadAsBytes(path);
    }

}
