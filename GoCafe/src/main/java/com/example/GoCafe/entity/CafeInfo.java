package com.example.GoCafe.entity;

import com.example.GoCafe.dto.CafeInfoForm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Table(name = "cafe_info")
@Getter @Setter
@AllArgsConstructor
public class CafeInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_info_id", nullable = false, unique = true)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Column(name = "notice", length = 20)
    private String notice;

    @Column(name = "description", length = 500) // 변경
    private String info;

    @Column(name = "open_time", length = 7)
    private String openTime;

    @Column(name = "close_time", length = 7)
    private String closeTime;

    @Column(name = "holiday", length = 7)
    private String holiday;

    public void update(CafeInfoForm dto) {
        if (dto.getCafeNotice() != null) this.notice = dto.getCafeNotice();
        if (dto.getCafeInfo() != null) this.info = dto.getCafeInfo();
        if (dto.getCafeOpenTime() != null) this.openTime = dto.getCafeOpenTime();
        if (dto.getCafeCloseTime() != null) this.closeTime = dto.getCafeCloseTime();
        if (dto.getCafeHoliday() != null) this.holiday = dto.getCafeHoliday();
    }
}