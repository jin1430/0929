package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.UserNeeds;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class NeedsForm {

    private Long id;
    private Member member;
    private String categoryCode;
    private String code;
    private boolean isNecessary;

    public UserNeeds toEntity() {
        return new UserNeeds(
                id,
                member,
                categoryCode,
                code,
                isNecessary
        );
    }
}
