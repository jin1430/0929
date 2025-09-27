package com.example.GoCafe.domain;

public enum RoleKind {
    ADMIN, MEMBER, PRO, OWNER;

    // (선택) 화면 라벨
    public String label() {
        return switch (this) {
            case ADMIN -> "관리자";
            case MEMBER -> "일반";
            case PRO -> "프로";
            case OWNER -> "점주";
        };
    }
}

