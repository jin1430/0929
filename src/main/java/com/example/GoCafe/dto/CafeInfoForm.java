package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가

@Getter
@Setter // Setter 추가
@NoArgsConstructor
@AllArgsConstructor
public class CafeInfoForm {

    private Long cafeInfoId;
    private Cafe cafe;
    private String notice; // 필드명 통일
    private String cafeInfo;
    private String openTime;   // ★★★ 필드명 변경 ★★★
    private String closeTime;  // ★★★ 필드명 변경 ★★★
    private String holiday;    // ★★★ 필드명 변경 ★★★

    // ============== toEntity, fromEntity 메소드 수정 =================

    public CafeInfo toEntity() {
        return new CafeInfo(
                this.cafeInfoId,
                this.cafe,
                this.notice,
                this.cafeInfo,
                this.openTime,
                this.closeTime,
                this.holiday
        );
    }

    public static CafeInfoForm fromEntity(CafeInfo entity) {
        if (entity == null) {
            return new CafeInfoForm();
        }
        return new CafeInfoForm(
                entity.getId(),
                entity.getCafe(),
                entity.getNotice(),
                entity.getCafeInfo(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.getHoliday()
        );
    }
}