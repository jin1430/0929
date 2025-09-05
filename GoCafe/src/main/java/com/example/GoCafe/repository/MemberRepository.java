package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberEmail(String memberEmail);
    // ✅ 닉네임 중복 체크용
    boolean existsByMemberNickname(String memberNickname);
    boolean existsByMemberEmail(String memberEmail);

    Optional<Member> findByMemberNickname(String candidate);

}