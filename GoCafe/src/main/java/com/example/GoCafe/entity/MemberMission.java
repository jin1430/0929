package com.example.GoCafe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name="MEMBER_MISSION",
        uniqueConstraints=@UniqueConstraint(name="UX_MM_UNIQUE_PARTICIPATION", columnNames={"MISSION_ID","MEMBER_ID"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberMission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="MISSION_ID", nullable=false)
    private Mission mission;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="MEMBER_ID", nullable=false)
    private Member member;

    @Column(length=20, nullable=false) private String status; // REQUESTED/ACCEPTED/COMPLETED/CANCELED
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="REVIEW_ID")
    private Review review;

    @Column(length=500) private String note;
    @Column(length=300) private String proofUrl;

    @PrePersist
    void prePersist() { if (status == null) status = "REQUESTED"; }
}