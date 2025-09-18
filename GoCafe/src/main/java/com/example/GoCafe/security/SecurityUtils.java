package com.example.GoCafe.security;

import com.example.GoCafe.domain.RoleKind;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class SecurityUtils {
    public static Collection<? extends GrantedAuthority> toAuthorities(RoleKind roleKind) {
        // ADMIN -> ROLE_ADMIN, MEMBER -> ROLE_MEMBER, PRO -> ROLE_PRO
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleKind.name()));
    }
}