package com.example.GoCafe.repository;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.entity.Cafe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {

    // ✅ 엔티티 필드명 'cafeStatus'에 맞춰 수정
    List<Cafe> findByCafeStatus(CafeStatus status);

    boolean existsByName(String cafeName);

    // ✅ 엔티티 필드명 'phoneNumber'에 맞춰 수정
    boolean existsByPhoneNumber(String phoneNumber);

    List<Cafe> findTop8ByOrderByViewsDesc();

    // ✅ 엔티티 필드명 'cafeStatus'에 맞춰 수정
    List<Cafe> findByCafeStatusOrderByViewsDesc(CafeStatus status, Pageable pageable);

    // ✅ 엔티티 필드명 'cafeStatus'에 맞춰 수정
    List<Cafe> findByCafeStatusAndNameContainingOrCafeStatusAndAddressContaining(
            CafeStatus status1, String name, CafeStatus status2, String address);

    // ✅ 엔티티 필드명 'cafeStatus'에 맞춰 수정
    long countByCafeStatus(CafeStatus status);

    boolean existsByAddress(String address);

    // Owner-visible (pending included)
    List<Cafe> findByOwner_IdAndCafeStatus(Long ownerId, com.example.GoCafe.domain.CafeStatus status);

    // Keyword search within owner's cafés by status
    List<Cafe> findByOwner_IdAndCafeStatusAndNameContainingOrOwner_IdAndCafeStatusAndAddressContaining(Long ownerId1, CafeStatus status1, String name, Long ownerId2, CafeStatus status2, String address);

    // Admin-wide keyword search (all statuses)
    List<Cafe> findByNameContainingOrAddressContaining(String name, String address);
}