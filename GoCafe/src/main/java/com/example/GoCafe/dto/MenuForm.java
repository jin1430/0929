package com.example.GoCafe.dto;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.entity.MenuCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuForm {

    /* ===================================
     * ↓↓ 기존 코드 (그대로 유지) ↓↓
     * =================================== */
    private Long menuId;
    private Cafe cafe;
    private MenuCategory category;
    private String menuPhoto;       // 메뉴 url (기존 필드)
    private boolean menuNew;         // 신메뉴 여부 (기존 필드)
    private boolean menuRecommanded; // 추천 메뉴 여부 (기존 필드)

    public Menu toEntity() {
        return new Menu(
                menuId,
                cafe,
                category,
                menuName,
                menuPrice,
                menuNew,
                menuRecommanded,
                menuPhoto
        );
    }

    /* ===================================
     * ↓↓ ## 추가된 필드와 메서드 ## ↓↓
     * =================================== */

    // owner-modal.mustache의 form input name과 일치하는 필드
    private String menuName;
    private Integer menuPrice;
    private String photoUrl; // 사진 URL을 직접 입력받는 경우
    private Boolean isNew = false;
    private Boolean isRecommended = false;

    /**
     * Service에서 메뉴를 추가할 때 사용하는 새로운 toEntity 메서드.
     * @param cafe 이 메뉴가 속할 Cafe 엔티티
     * @param generatedPhotoUrl 파일 업로드 등을 통해 생성된 최종 이미지 URL
     * @return 생성된 Menu 엔티티
     */
    public Menu toEntity(Cafe cafe, String generatedPhotoUrl) {
        return Menu.builder()
                .cafe(cafe)
                .name(this.menuName)
                .price(this.menuPrice)
                .photo(generatedPhotoUrl)
                .isNew(this.isNew != null && this.isNew)
                .isRecommended(this.isRecommended != null && this.isRecommended)
                .build();
    }
}