package com.example.GoCafe.entity;

import com.example.GoCafe.domain.CafeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
@Getter
@Setter
@AllArgsConstructor
public class Cafe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member owner;

    @Column(name = "name", nullable = false, length = 10)
    private String name;

    @Column(name = "address", nullable = false, length = 60)
    private String address;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15) // 변경
    private String phoneNumber;

    @Column(name = "creation_date", nullable = false) // 변경
    private LocalDate creationDate;

    @Column(name = "views", nullable = false)
    private Long views = 0L;

    @Column(name = "business_code", nullable = false, length = 10) // 변경
    private String businessCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cafe_status", nullable = false, length = 10) // 변경
    private CafeStatus cafeStatus = CafeStatus.PENDING;

    @Column(name = "biz_doc", length = 255)
    private String bizDoc;

    public boolean isApproved() { return cafeStatus == CafeStatus.APPROVED; }
    public boolean isPending()  { return cafeStatus == CafeStatus.PENDING; }
    public boolean isRejected() { return cafeStatus == CafeStatus.REJECTED; }

    @JsonIgnore
    @OneToMany(mappedBy = "cafe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();
}