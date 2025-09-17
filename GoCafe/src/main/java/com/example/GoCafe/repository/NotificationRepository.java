package com.example.GoCafe.repository;

import com.example.GoCafe.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByRecipient_EmailAndReadIsFalse(String email);
    List<Notification> findTop20ByRecipient_EmailOrderByCreatedAtDesc(String email);
    List<Notification> findByRecipient_EmailAndReadIsFalseOrderByCreatedAtDesc(String email);

    @Query("""
      select n from Notification n
       where n.recipient.id = :memberId
         and n.cafe.id = :cafeId
         and n.message like :prefix%
       order by n.createdAt desc
    """)
    List<Notification> findByMemberCafeAndPrefix(@Param("memberId") Long memberId,
                                                 @Param("cafeId") Long cafeId,
                                                 @Param("prefix") String prefix);

    default Optional<Notification> latestMissionLog(Long memberId, Long cafeId) {
        List<Notification> list = findByMemberCafeAndPrefix(memberId, cafeId, "MISSION:");
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
