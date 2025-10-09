package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String memberEmail);
    Optional<Member> findByNickname(String candidate);

    boolean existsByNickname(String memberNickname);

}