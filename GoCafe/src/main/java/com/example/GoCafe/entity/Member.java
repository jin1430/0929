package com.example.GoCafe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Getter
@Setter
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "member_email", nullable = false, unique = true, length = 100)
    private String memberEmail;

    @Column(name = "member_password", nullable = false, length = 100)
    private String memberPassword;

    @Column(name = "member_nickname", nullable = false, unique = true, length = 8)
    private String memberNickname;

    @Column(name = "member_age")
    private Long memberAge;

    @Column(name = "member_gender", length = 1)
    private String memberGender;

    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole;

    @Column(name = "member_date", nullable = false)
    private java.time.LocalDateTime memberDate;

    @Column(name = "member_photo", length = 30)
    private String memberPhoto;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

}