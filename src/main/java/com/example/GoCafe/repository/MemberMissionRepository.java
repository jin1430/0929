package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.MemberMission;
import com.example.GoCafe.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberMissionRepository extends JpaRepository<MemberMission, Long> {

    // 특정 회원이 참여한 모든 미션 정보를 조회합니다. (페이징 가능)
    Page<MemberMission> findByMember(Member member, Pageable pageable);

    // 특정 미션에 참여한 모든 회원 정보를 조회합니다.
    List<MemberMission> findByMission(Mission mission);

    // 특정 회원의 특정 미션 참여 정보를 조회합니다.
    Optional<MemberMission> findByMemberAndMission(Member member, Mission mission);

    // 특정 회원이 특정 미션에 참여했는지 여부를 확인합니다.
    boolean existsByMemberAndMission(Member member, Mission mission);

    // 특정 회원이 참여한 미션 중 특정 상태(status)에 있는 정보만 조회합니다.
    List<MemberMission> findByMemberAndStatus(Member member, String status);
}