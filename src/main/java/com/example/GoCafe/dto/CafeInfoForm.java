package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CafeInfoForm {

    private Long cafeInfoId;
    private Cafe cafe;
    private String cafeNotice;
    private String cafeInfo;
    private String cafeOpenTime;
    private String cafeCloseTime;
    private String cafeHoliday;

    // ============== Setter 메서드 수정/추가 (공백 처리) =================

    private String toNullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public void setCafeInfoId(Long cafeInfoId) {
        this.cafeInfoId = cafeInfoId;
    }

    public void setCafe(Cafe cafe) {
        this.cafe = cafe;
    }

    public void setCafeNotice(String cafeNotice) {
        this.cafeNotice = toNullIfBlank(cafeNotice);
    }

    public void setCafeInfo(String cafeInfo) {
        this.cafeInfo = toNullIfBlank(cafeInfo);
    }

    public void setCafeOpenTime(String cafeOpenTime) {
        this.cafeOpenTime = toNullIfBlank(cafeOpenTime);
    }

    public void setCafeCloseTime(String cafeCloseTime) {
        this.cafeCloseTime = toNullIfBlank(cafeCloseTime);
    }

    public void setCafeHoliday(String cafeHoliday) {
        this.cafeHoliday = toNullIfBlank(cafeHoliday);
    }

    // =======================================================

    public CafeInfo toEntity() {
        return new CafeInfo(
                cafeInfoId,
                cafe,
                cafeNotice,
                cafeInfo,
                cafeOpenTime,
                cafeCloseTime,
                cafeHoliday
        );
    }
}