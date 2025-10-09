package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.service.MenuService;
import com.example.GoCafe.support.EntityIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/owner/cafes")
public class CafeMenuController {

    private final MenuService menuService;

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/menus")
    public String addMenu(@PathVariable Long cafeId,
                          @RequestParam String menuName,
                          @RequestParam(required = false) Integer menuPrice,
                          @RequestParam(required = false) MultipartFile menuPhoto,           // 파일 저장은 추후
                          @RequestParam(required = false, name = "photoUrl") String photoUrl, // URL 우선
                          @RequestParam(required = false, name = "isNew") Boolean isNew,
                          @RequestParam(required = false, name = "isRecommended") Boolean isRecommended,
                          RedirectAttributes ra) {

        Menu m = new Menu();
        // Cafe PK만 설정한 레퍼런스
        Cafe cafeRef = new Cafe();
        EntityIdUtil.setId(cafeRef, cafeId);
        m.setCafe(cafeRef);

        m.setName(menuName);
        m.setPrice(menuPrice == null ? 0 : menuPrice);
        m.setNew(isNew != null && isNew);
        m.setRecommended(isRecommended != null && isRecommended);

        // 사진은 우선 URL만
        if (photoUrl != null && !photoUrl.isBlank()) {
            m.setPhoto(photoUrl.trim());
        } else if (menuPhoto != null && !menuPhoto.isEmpty()) {
            // TODO: 파일 저장 후 공개 URL 세팅
            // String url = storage.save(menuPhoto);
            // m.setPhoto(url);
        }

        menuService.create(m);
        ra.addFlashAttribute("success", "메뉴가 추가되었습니다.");
        return "redirect:/cafes/" + cafeId;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/menus/{menuId}/delete")
    public String deleteMenu(@PathVariable Long cafeId,
                             @PathVariable Long menuId,
                             RedirectAttributes ra) {
        menuService.delete(menuId);
        ra.addFlashAttribute("success", "메뉴가 삭제되었습니다.");
        return "redirect:/cafes/" + cafeId;
    }
}
