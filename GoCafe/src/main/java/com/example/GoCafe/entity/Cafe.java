    package com.example.GoCafe.entity;

    import com.example.GoCafe.domain.CafeStatus;
    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

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
        private Long cafeId;

        @Column(name = "cafe_owner_id")
        private Long cafeOwnerId;

        @Column(name = "cafe_name", nullable = false, unique = true, length = 10)
        private String cafeName;

        @Column(name = "cafe_address", nullable = false, unique = true, length = 60)
        private String cafeAddress;

        @Column(name = "cafe_lat", nullable = false)
        private Double cafeLat;

        @Column(name = "cafe_lon", nullable = false)
        private Double cafeLon;

        @Column(name = "cafe_number", nullable = false, unique = true, length = 15)
        private String cafeNumber;

        @Column(name = "cafe_date", nullable = false)
        private java.time.LocalDateTime cafeDate;

        @Column(name = "cafe_views", nullable = false)
        private Long cafeViews = 0L;

        @Column(name = "cafe_photo", length = 255)
        private String cafePhoto;

        @OneToMany(mappedBy = "cafe", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<CafeImage> images = new ArrayList<>();

        @Column(name = "cafe_code", nullable = false, length = 10)
        private String cafeCode;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 10)
        private CafeStatus status = CafeStatus.PENDING;


        @PrePersist
        public void prePersist() {
            if (cafeViews == null) cafeViews = 0L;
        }
        public void addImage(CafeImage img) {
            images.add(img);
            img.setCafe(this);
            if (img.isMain() || cafePhoto == null) {
                this.cafePhoto = img.getUrl(); // 메인이거나 최초 업로드면 대표사진 갱신
            }
        }

        // ✅ 사업자 증빙 문서 URL 저장용 필드 추가
        @Column(name = "biz_doc", length = 255)
        private String bizDoc;

        public boolean isApproved() { return status == CafeStatus.APPROVED; }
        public boolean isPending()  { return status == CafeStatus.PENDING;  }
        public boolean isRejected() { return status == CafeStatus.REJECTED; }
    }