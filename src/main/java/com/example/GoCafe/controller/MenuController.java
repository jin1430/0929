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
import org.springframework.stereotype.Controller; // 1. @RestController에서 변경
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller // 1. @RestController를 @Controller로 변경
@RequiredArgsConstructor
@RequestMapping("/cafes/menus") // 2. API 충돌 방지를 위해 기본 경로 변경
public class MenuController {

    private final MenuService menuService;
    private final MenuVectorService menuVectorService;
    private final CafeService cafeService;
    private final FileStorageService storage;

    @PostMapping("/add") // 2. 새로운 추가 경로
    public String createWithPhoto(
            @Valid @ModelAttribute("form") com.example.GoCafe.dto.MenuCreate form,
            @RequestParam(value = "file", required = false) MultipartFile file,
            RedirectAttributes ra
    ) {
        Long cafeId = form.getCafeId();
        Cafe cafe = cafeService.findById(cafeId);
        Menu m = form.toEntity(cafe);

        if (file != null && !file.isEmpty()) {
            String url = storage.save(file, "menus");
            m.setPhoto(url);
        }

        Menu saved = menuService.create(m);
        menuVectorService.upsertForMenu(saved);

        ra.addFlashAttribute("msg", "메뉴가 추가되었습니다.");

        // 3. JSON 응답 대신 상세 페이지로 리다이렉트
        return "redirect:/cafes/" + cafeId;
    }

    // 기존 updateMenu 메서드는 그대로 유지됩니다.
    // 단, JSON 대신 페이지 리다이렉트를 위해 반환 타입만 수정합니다.
    @PatchMapping(value = "/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateMenu( // 4. 반환 타입을 String으로 변경
                              @PathVariable Long menuId,
                              @Valid @ModelAttribute com.example.GoCafe.dto.MenuUpdate form,
                              @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Menu menu = menuService.findById(menuId);
        String oldName = menu.getName();

        if (form.getName() != null) menu.setName(form.getName());
        if (form.getPrice() != null) menu.setPrice(form.getPrice());
        if (form.getIsNew() != null) menu.setNew(form.getIsNew());
        if (form.getIsRecommended() != null) menu.setRecommended(form.getIsRecommended());

        if (file != null && !file.isEmpty()) {
            String url = storage.save(file, "menus");
            menu.setPhoto(url);
        }

        Menu saved = menuService.update(menuId, menu);

        if (!saved.getName().equals(oldName)) {
            menuVectorService.upsertForMenu(saved);
        }

        // 4. JSON 응답 대신 상세 페이지로 리다이렉트
        return "redirect:/cafes/" + saved.getCafe().getId();
    }
}