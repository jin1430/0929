package com.example.GoCafe.dto;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CafeForm {

    // 서버에서 로그인 사용자로 덮어쓰기 권장
    private Member owner;

    private String name;
    private String address;
    private Double lat;
    private Double lon;
    private String number;
    private Long views;
    private String code;
    private String bizDoc;

    public Cafe toEntity() {
        Cafe cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName(name);
        cafe.setAddress(address);
        cafe.setLat(lat);
        cafe.setLon(lon);
        cafe.setNumber(number);
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(views != null ? views : 0L);
        cafe.setCode(code);
        cafe.setBizDoc(bizDoc);
        cafe.setStatus(CafeStatus.PENDING);
        return cafe;
    }
}
