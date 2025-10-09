// src/main/java/com/example/GoCafe/controller/MenuController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.FileStorageService;
import com.example.GoCafe.service.MenuService;
import com.example.GoCafe.service.MenuVectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;
    private final MenuVectorService menuVectorService;
    private final CafeService cafeService;              // Cafe 주입 필요
    private final FileStorageService storage;           // LocalFileStorageService

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createWithPhoto(
            @Valid @ModelAttribute com.example.GoCafe.dto.MenuCreate form,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        // 1) 카페 조회
        Cafe cafe = cafeService.findById(form.getCafeId());

        // 2) DTO -> 엔티티 변환
        Menu m = form.toEntity(cafe);

        // 3) 파일이 있으면 저장 후 URL 세팅
        if (file != null && !file.isEmpty()) {
            String url = storage.save(file, "menus"); // /uploads/menus/uuid.jpg
            m.setPhoto(url);
        }

        // 4) 저장
        Menu saved = menuService.create(m);

        // 5) 벡터 생성 (메뉴명 기반)
        menuVectorService.upsertForMenu(saved);

        // 6) 응답
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "price", saved.getPrice(),
                "isNew", saved.isNew(),
                "isRecommended", saved.isRecommended(),
                "photo", saved.getPhoto()
        ));
    }

    @PatchMapping(value = "/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMenu(
            @PathVariable Long menuId,
            @Valid @ModelAttribute com.example.GoCafe.dto.MenuUpdate form,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        // 1) 기존 메뉴 조회
        Menu menu = menuService.findById(menuId);

        // 2) 변경 전 이름(벡터 재계산 여부 판단용)
        String oldName = menu.getName();

        // 3) 부분 업데이트 적용
        if (form.getName() != null) menu.setName(form.getName());
        if (form.getPrice() != null) menu.setPrice(form.getPrice());
        if (form.getIsNew() != null) menu.setNew(form.getIsNew());
        if (form.getIsRecommended() != null) menu.setRecommended(form.getIsRecommended());

        // 4) 사진 교체(선택)
        if (file != null && !file.isEmpty()) {
            String url = storage.save(file, "menus");
            menu.setPhoto(url);
        }

        // 5) 저장
        Menu saved = menuService.update(menuId, menu); // 없으면 create와 동일하게 저장하는 메서드가 있다면 그걸 사용

        // 6) 이름이 바뀐 경우 벡터 재계산(또는 항상 재계산해도 무방)
        if (!saved.getName().equals(oldName)) {
            menuVectorService.upsertForMenu(saved);
        }

        // 7) 응답
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "price", saved.getPrice(),
                "isNew", saved.isNew(),
                "isRecommended", saved.isRecommended(),
                "photo", saved.getPhoto()
        ));
    }


}
