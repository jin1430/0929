// src/main/java/com/example/GoCafe/controller/MenuController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.FileStorageService;
import com.example.GoCafe.service.MenuService;
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
    private final CafeService cafeService;              // Cafe 주입 필요
    private final FileStorageService storage;           // LocalFileStorageService

    // 사진 필수: multipart/form-data
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createWithPhoto(
            @RequestParam("cafeId") Long cafeId,
            @RequestParam("name") String name,
            @RequestParam("price") int price,
            @RequestParam(value = "isNew", defaultValue = "false") boolean isNew,
            @RequestParam(value = "isRecommended", defaultValue = "false") boolean isRecommended,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "menu photo is required"));
        }

        Cafe cafe = cafeService.findById(cafeId); // 없으면 내부에서 예외
        String url = storage.save(file, "menus"); // /uploads/menus/uuid.jpg

        Menu m = new Menu();
        m.setCafe(cafe);
        m.setName(name);
        m.setPrice(price);
        m.setNew(isNew);
        m.setRecommended(isRecommended);
        m.setPhoto(url);                             // ✅ 필수

        Menu saved = menuService.create(m);
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
