package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.UserNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNeedsRepository extends JpaRepository<UserNeeds, Long> {

    // 조회
    List<UserNeeds> findByMember(Member member);

    // ✅ 정렬 포함 조회: categoryCode ASC, code ASC
    List<UserNeeds> findByMember_IdOrderByCategoryCodeAscCodeAsc(Long memberId);

    List<UserNeeds> findByMember_Id(Long memberId);

    List<UserNeeds> findByMemberAndCategoryCode(Member member, String categoryCode);

    // ✅ 고유 조회: 엔티티는 'code' 필드
    Optional<UserNeeds> findByMemberAndCategoryCodeAndCode(Member member, String categoryCode, String code);

    // 삭제
    void deleteByMember(Member member);
    void deleteByMember_Id(Long memberId);

    @Modifying
    @Query("delete from UserNeeds n where n.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    List<UserNeeds> findByMemberId(Long memberId);
}
