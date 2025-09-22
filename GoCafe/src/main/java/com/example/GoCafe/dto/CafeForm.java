// src/main/java/com/example/GoCafe/dto/CafeForm.java
package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CafeForm {

    @NotBlank
    @Size(max = 10)
    private String name;

    @NotBlank
    @Size(max = 10)
    private String businessCode;   // 🔥 폼 name="businessCode"

    @NotBlank
    @Size(max = 60)
    private String address;

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;

    @NotBlank
    @Size(max = 15)
    private String phoneNumber;    // 🔥 폼 name="phoneNumber"

    // 미입력 가능 → 서버에서 기본값(LocalDate.now()) 처리
    private LocalDate creationDate;

    // 업로더에서 hidden으로 담아올 수도 있게 선택 필드 유지
    private String bizDoc;

    // ===== getters / setters =====
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

    // ===== 매핑 =====
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
}
