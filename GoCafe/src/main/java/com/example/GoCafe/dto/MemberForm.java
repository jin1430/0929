package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MemberForm {

    // 업데이트 시에만 사용; 생성 시 null
    private Long id;

    private String email;
    private String password;
    private String nickname;
    private Long age;
    private String gender;
    private String roleKind;
    private LocalDateTime createdAt;
    private String photo;
    private Long tokenVersion;

    public Member toEntity() {
        Member m = new Member();
        m.setId(id);                           // null이면 새로 생성
        m.setEmail(email);
        m.setPassword(password);
        m.setNickname(nickname);
        m.setAge(age);
        m.setGender(gender);
        m.setRoleKind(roleKind);               // @Column(name="role_kind")
        m.setPhoto(photo);
        if (tokenVersion != null) m.setTokenVersion(tokenVersion); // 엔티티 기본 0L이면 생략 가능
        return m;
    }
}
