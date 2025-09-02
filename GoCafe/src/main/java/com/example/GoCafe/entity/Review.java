// Review.java
package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @Column(nullable = false, length = 1000)
    private String reviewContent;

    @Builder.Default
    private int reviewGood = 0;

    @Builder.Default
    private int reviewBad = 0;

    // ✅ Member 연관관계 (작성자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 메타 정보
    private Integer waitingTime;   // 분
    private String companionType;  // SOLO/FRIEND/DATE/FAMILY/BUSINESS
    private Integer taste;         // 1~5
    private String sentiment;      // "GOOD" or "BAD"
    private String likedTagCsv;    // "맛있어요,친절해요" 등

    private LocalDateTime reviewDate;

    @Transient
    private List<ReviewTag> tags = new ArrayList<>();


    @Transient
    @ElementCollection
    @CollectionTable(name = "review_photos", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    // ✅ 닉네임을 뷰에서 간단히 쓰고 싶을 때: review.memberNickname 처럼 호출 가능
    @Transient
    public String getMemberNickname() {
        return (member != null ? member.getMemberNickname() : null);
    }

    @PrePersist
    void onCreate() {
        if (reviewDate == null) reviewDate = LocalDateTime.now();
    }

    @Transient
    public String getReviewDateFmt() {
        return reviewDate != null
                ? reviewDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                : "";
    }
}
