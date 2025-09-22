package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.UserNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNeedsRepository extends JpaRepository<UserNeeds, Long> {

    // 특정 회원의 모든 필요사항을 조회합니다.
    List<UserNeeds> findByMember(Member member);

    // 특정 회원의 필요사항 중 '필수'로 표시된 것만 조회합니다.
    List<UserNeeds> findByMemberAndIsNecessaryTrue(Member member);

    // 특정 회원의 특정 카테고리에 속한 필요사항들을 조회합니다.
    List<UserNeeds> findByMemberAndCategoryCode(Member member, String categoryCode);

    // 특정 회원의 특정 필요사항(code) 정보를 조회합니다.
    Optional<UserNeeds> findByMemberAndCode(Member member, String code);
}