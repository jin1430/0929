package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.entity.MenuImage;
import com.example.GoCafe.repository.MenuImageRepository;
import com.example.GoCafe.repository.MenuRepository;
import com.example.GoCafe.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuImageController {

    private final MenuRepository menuRepository;
    private final MenuImageRepository menuImageRepository;
    private final FileStorageService storage;

    @PostMapping("/{menuId}/photos")
    @Transactional
    public ResponseEntity<?> upload(@PathVariable Long menuId,
                                    @RequestParam("files") List<MultipartFile> files,
                                    @RequestParam(value = "setMain", required = false, defaultValue = "false") boolean setMain) throws Exception {

        Menu menu = menuRepository.findById(menuId).orElseThrow();

        for (MultipartFile f : files) {
            var sf = storage.store(f, "menus/" + menuId);
            MenuImage img = new MenuImage();
            img.setMenu(menu);
            img.setStoredName(sf.storedName());
            img.setOriginalName(sf.originalName());
            img.setUrl(sf.url());
            img.setContentType(sf.contentType());
            img.setSizeBytes(sf.sizeBytes());
            img.setMain(false);
            menuImageRepository.save(img);

            if (menu.getMenuPhoto() == null || setMain) {
                menu.setMenuPhoto(sf.url());
                img.setMain(true);
            }
        }
        return ResponseEntity.ok(menuImageRepository.findByMenu_MenuId(menuId));
    }

    @PatchMapping("/photos/{photoId}/main")
    @Transactional
    public ResponseEntity<?> setMain(@PathVariable Long photoId) {
        MenuImage target = menuImageRepository.findById(photoId).orElseThrow();
        Menu menu = target.getMenu();
        menuImageRepository.findByMenu_MenuId(menu.getMenuId())
                .forEach(i -> i.setMain(i.getId().equals(photoId)));
        menu.setMenuPhoto(target.getUrl());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{menuId}/photos")
    public List<MenuImage> list(@PathVariable Long menuId) {
        return menuImageRepository.findByMenu_MenuId(menuId);
    }

    @DeleteMapping("/photos/{photoId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long photoId) {
        menuImageRepository.deleteById(photoId);
        return ResponseEntity.noContent().build();
    }
}
