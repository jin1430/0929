package com.example.GoCafe.dto;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.entity.Cafe;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CafeForm {

    private Long   cafeOwnerId;    // 서버에서 로그인 사용자로 덮어쓰기 권장
    private String cafeName;
    private String cafeAddress;
    private Double cafeLat;
    private Double cafeLon;
    private String cafeNumber;
    private Long   cafeViews;
    private String cafePhoto;
    private String cafeCode;
    private String bizDoc;         // Cafe에 필드 없다면 setBizDoc 호출 지워도 됨
    private String stats;          // 사용 안하면 제거 가능

    public Cafe toEntity() {
        Cafe c = new Cafe();
        c.setCafeOwnerId(cafeOwnerId);
        c.setCafeName(cafeName);
        c.setCafeAddress(cafeAddress);
        c.setCafeLat(cafeLat);
        c.setCafeLon(cafeLon);
        c.setCafeNumber(cafeNumber);
        c.setCafeDate(LocalDateTime.now());
        c.setCafeViews(cafeViews != null ? cafeViews : 0L);
        c.setCafePhoto(cafePhoto);
        c.setCafeCode(cafeCode);
        try { c.setBizDoc(bizDoc); } catch (Throwable ignore) {}
        try {
            if (c.getImages() == null) c.setImages(new ArrayList<>());
        } catch (Throwable ignore) {}
        c.setStatus(CafeStatus.PENDING);
        return c;
    }
}
