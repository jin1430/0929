package com.example.GoCafe.service;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.UserNeeds;
import com.example.GoCafe.repository.MemberRepository;
import com.example.GoCafe.repository.UserNeedsRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserNeedsRepository userNeedsRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt 빈 등록 가정

    private final ObjectMapper om = new ObjectMapper();
    private static final String CODE_IMPORTANCE = "__IMPORTANCE__";

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public Member create(Member entity) {
        EntityIdUtil.setId(entity, null);
        return memberRepository.save(entity);
    }

    @Transactional
    public Member update(Long id, Member entity) {
        if (!memberRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return memberRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        memberRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
    }

    // ===== 여기에 추가: 프로필 수정(+선택적 비번변경), 탈퇴, 토큰버전 =====

    /**
     * 프로필 수정 + (옵션) 비밀번호 변경을 한 번에 처리.
     * @return changedPassword: 비밀번호가 실제로 변경되었으면 true
     */
    @Transactional
    public boolean updateSelf(Long memberId,
                              String nickname,
                              Long age,
                              String gender,
                              String photo,
                              String currentPasswordNullable,
                              String newPasswordNullable) {

        Member m = findById(memberId);

        // 닉네임 변경 중복 체크
        if (nickname != null && !nickname.isBlank()
                && !nickname.equals(m.getNickname())
                && memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 일반 프로필 변경
        if (nickname != null && !nickname.isBlank()) m.setNickname(nickname);
        if (age != null) m.setAge(age);
        if (gender != null && !gender.isBlank()) m.setGender(gender);
        if (photo != null) m.setPhoto(photo);

        // 비번 변경 (둘 다 들어온 경우에만 시도)
        boolean changePw = (currentPasswordNullable != null && !currentPasswordNullable.isBlank()
                && newPasswordNullable != null && !newPasswordNullable.isBlank());

        if (changePw) {
            if (!passwordEncoder.matches(currentPasswordNullable, m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            m.setPassword(passwordEncoder.encode(newPasswordNullable));
        }

        return false;
    }

    /** 자기 자신 탈퇴 */
    @Transactional
    public void withdrawSelf(Long memberId) {
        delete(memberId);
    }

    /** 토큰버전 증가 (내부용) */
    private void bumpTokenVersionInternal(Member m) {
        Long v = (m.getTokenVersion() == null ? 0L : m.getTokenVersion());
        m.setTokenVersion(v + 1);
    }

}
