package com.example.GoCafe.apiController;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafeInfoRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.service.CafeInfoService;
import com.example.GoCafe.support.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafe-infos")
public class CafeInfoApiController {

    private final CafeInfoService service;
    private final CafeInfoRepository cafeInfoRepository;
    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;

    /** 카페별 상세정보 단건 조회 (없으면 404) */
    @GetMapping("/by-cafe/{cafeId}")
    public ResponseEntity<CafeInfo> byCafe(@PathVariable Long cafeId) {
        return service.findByCafeId(cafeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /** 생성/수정 upsert (점주만) */
    @PostMapping("/upsert/{cafeId}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfo upsert(@PathVariable Long cafeId,
                           @RequestBody @Valid CafeInfo body,
                           Authentication auth) {
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("카페를 찾을 수 없습니다."));
        ensureOwner(auth, cafe);

        body.setCafe(cafe); // 관계 설정
        return service.upsertByCafeId(cafeId, body);
    }

    /** 수정 (점주만) */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfo update(@PathVariable Long id,
                           @RequestBody @Valid CafeInfo body,
                           Authentication auth) {
        CafeInfo cur = cafeInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CafeInfo 없음"));
        ensureOwner(auth, cur.getCafe());

        body.setId(id);
        body.setCafe(cur.getCafe());
        return service.update(id, body);
    }

    /** 삭제 (점주만) */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable Long id, Authentication auth) {
        CafeInfo cur = cafeInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CafeInfo 없음"));
        ensureOwner(auth, cur.getCafe());
        service.delete(id);
    }

    // ===== 권한 보조 =====
    private void ensureOwner(Authentication auth, Cafe cafe) {
        if (auth == null || !auth.isAuthenticated())
            throw new org.springframework.security.access.AccessDeniedException("로그인 필요");

        String email = auth.getName();
        Member me = memberRepository.findByEmail(email).orElse(null);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        boolean isOwner = me != null && cafe.getOwner() != null
                && cafe.getOwner().getId().equals(me.getId());

        if (!(isOwner || isAdmin))
            throw new org.springframework.security.access.AccessDeniedException("점주만 가능합니다.");
    }
}
