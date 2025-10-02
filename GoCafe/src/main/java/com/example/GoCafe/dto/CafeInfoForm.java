package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
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

    // 기존 toEntity() 메서드 (그대로 둡니다)
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

    // ## 아래 새로운 메서드만 추가! ##
    /**
     * Service 계층에서 조회한 Cafe 엔티티를 받아 CafeInfo 엔티티를 생성합니다.
     * 이 메서드를 사용하는 것이 더 권장됩니다.
     */
    public CafeInfo toEntity(Cafe cafeEntity) {
        return new CafeInfo(
                this.cafeInfoId,
                cafeEntity, // 파라미터로 받은 cafe를 사용
                this.cafeNotice,
                this.cafeInfo,
                this.cafeOpenTime,
                this.cafeCloseTime,
                this.cafeHoliday
        );
    }
}