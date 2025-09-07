package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.repository.MenuRepository;
import com.example.GoCafe.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuRepository menuRepository;
    private final FileStorageService storage;

    //메뉴 사진 업로드
    @PostMapping(value = "/{menuId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadMenuPhoto(@PathVariable Long menuId,
                                             @RequestParam("file") MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 없습니다.");
        }

        Menu menu = menuRepository.findById(menuId).orElseThrow();
        var sf = storage.store(file, "menus/" + menuId);
        menu.setPhoto(sf.url());

        return ResponseEntity.ok(Map.of("menuId", menu.getId(), "photoUrl", sf.url()));
    }

    // 메뉴 사진 URL 조회
    @GetMapping("/{menuId}/photos")
    public ResponseEntity<?> getPhoto(@PathVariable Long menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "menuId", menu.getId(),
                "photoUrl", menu.getPhoto()
        ));
    }

    // 메뉴 사진 제거
    @DeleteMapping("/{menuId}/photos")
    @Transactional
    public ResponseEntity<?> clearPhoto(@PathVariable Long menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow();
        // 기존 업로드 파일을 스토리지에서 지우고 싶다면 storage.delete(...)를 여기에 추가하세요.
        menu.setPhoto(null);
        return ResponseEntity.noContent().build();
    }
}
