package com.example.GoCafe.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoleKind {
    ADMIN, MEMBER, PRO, OWNER;

    public String label() {
        return switch (this) {
            case ADMIN -> "관리자";
            case MEMBER -> "일반";
            case PRO   -> "프로";
            case OWNER -> "점주";
        };
    }

    /** null/공백 허용 + 대소문자 무시 */
    public static RoleKind from(String s) {
        if (s == null || s.isBlank()) return MEMBER;
        return RoleKind.valueOf(s.trim().toUpperCase());
    }

    /** JSON 바인딩 시에도 같은 규칙 적용 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoleKind fromJson(String s) {
        return from(s);
    }
}
