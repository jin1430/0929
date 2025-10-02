package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CafeForm {

    /* ===================================
     * ↓↓ 기존 필드 및 메서드 (그대로 유지) ↓↓
     * =================================== */
    @NotBlank
    @Size(max = 10)
    private String name;

    @NotBlank
    @Size(max = 10)
    private String businessCode;

    @NotBlank
    @Size(max = 60)
    private String address;

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;

    @NotBlank
    @Size(max = 15)
    private String phoneNumber;

    private LocalDate creationDate;
    private String bizDoc;

    // ===== getters / setters (기존 코드) =====
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public String getBizDoc() { return bizDoc; }
    public void setBizDoc(String bizDoc) { this.bizDoc = bizDoc; }

    // ===== 매핑 (기존 코드) =====
    public Cafe toEntity() {
        Cafe c = new Cafe();
        c.setName(this.name);
        c.setBusinessCode(this.businessCode);
        c.setAddress(this.address);
        c.setLat(this.lat);
        c.setLon(this.lon);
        c.setPhoneNumber(this.phoneNumber);
        if (this.creationDate != null) {
            c.setCreationDate(this.creationDate);
        }
        if (this.bizDoc != null) {
            try { c.setBizDoc(this.bizDoc); } catch (Throwable ignored) {}
        }
        return c;
    }

    /* ## ↓↓↓ 여기에 새로운 메서드만 추가하세요! ↓↓↓ ## */
    /**
     * Cafe 엔티티의 현재 값을 DTO로 변환합니다.
     * (수정 폼에 기존 값을 채워주기 위한 용도)
     * @param cafe 원본 Cafe 엔티티
     * @return 값이 채워진 CafeForm DTO
     */
    public static CafeForm from(Cafe cafe) {
        CafeForm form = new CafeForm();
        form.setName(cafe.getName());
        form.setAddress(cafe.getAddress());
        form.setLat(cafe.getLat());
        form.setLon(cafe.getLon());
        form.setPhoneNumber(cafe.getPhoneNumber());
        form.setBusinessCode(cafe.getBusinessCode());
        form.setBizDoc(cafe.getBizDoc());
        form.setCreationDate(cafe.getCreationDate());
        return form;
    }
}