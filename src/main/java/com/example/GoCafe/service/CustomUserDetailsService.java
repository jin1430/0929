// src/main/java/com/example/GoCafe/service/CustomUserDetailsService.java
package com.example.GoCafe.service;

import com.example.GoCafe.domain.RoleKind;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.MemberRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // enum 기반 (null이면 기본 MEMBER)
        RoleKind kind = (m.getRoleKind() == null) ? RoleKind.MEMBER : m.getRoleKind();

        // 권한 상속: ADMIN/PRO => ROLE_MEMBER도 함께 부여
        List<GrantedAuthority> authorities = switch (kind) {
            case ADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_MEMBER")
            );
            case PRO -> List.of(
                    new SimpleGrantedAuthority("ROLE_PRO"),
                    new SimpleGrantedAuthority("ROLE_MEMBER")
            );
            default -> List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
        };

        return User.withUsername(m.getEmail())
                .password(m.getPassword())
                .authorities(authorities) // 이미 ROLE_ 접두어 포함
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
