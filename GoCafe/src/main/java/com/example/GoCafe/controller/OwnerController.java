package com.example.GoCafe.controller;

import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.dto.CafeInfoForm;
import com.example.GoCafe.dto.MenuForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.service.CafeInfoService;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.MenuService;
import com.example.GoCafe.support.EntityIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/owner/cafes")
public class OwnerController {

    private final CafeInfoService cafeInfoService;
    private final MenuService menuService;
    private final CafeService cafeService;
    /**
     * 카페 정보 생성/수정 (Upsert)
     */
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping(value = {"/{cafeId}/info/create", "/{cafeId}/info/update"})
    public String upsertCafeInfo(@PathVariable Long cafeId,
                                 @ModelAttribute CafeInfoForm form,
                                 RedirectAttributes ra) {

        Cafe cafeRef = new Cafe();
        EntityIdUtil.setId(cafeRef, cafeId);
        form.setCafe(cafeRef); // DTO에 Cafe 참조 설정

        CafeInfo entity = form.toEntity();

        // 길이 제한 안전 처리
        entity.setNotice(trim(form.getCafeNotice(), 20));
        entity.setInfo(trim(form.getCafeInfo(), 500));
        entity.setOpenTime(trim(form.getCafeOpenTime(), 7));
        entity.setCloseTime(trim(form.getCafeCloseTime(), 7));
        entity.setHoliday(trim(form.getCafeHoliday(), 7));

        cafeInfoService.upsertByCafeId(cafeId, entity);

        ra.addFlashAttribute("success", "카페 정보가 저장되었습니다.");
        return "redirect:/cafes/" + cafeId;
    }

    // --- 메뉴 관련 메서드 ---
    @PostMapping("/{cafeId}/menus")
    public String addMenu(@PathVariable Long cafeId,
                          @ModelAttribute MenuForm dto,
                          @RequestParam("menuPhoto") MultipartFile menuPhoto,
                          RedirectAttributes redirectAttributes) {
        try {
            menuService.addMenu(cafeId, dto, menuPhoto);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "메뉴 사진 업로드에 실패했습니다.");
        }
        return "redirect:/cafes/" + cafeId;
    }

    @PostMapping("/{cafeId}/menus/{menuId}/delete")
    public String deleteMenu(@PathVariable Long cafeId, @PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return "redirect:/cafes/" + cafeId;
    }

    @PostMapping("/{cafeId}/update")
    public String updateCafe(@PathVariable Long cafeId, @ModelAttribute CafeForm form) {
        cafeService.updateCafeByOwner(cafeId, form);
        return "redirect:/cafes/" + cafeId;
    }
    // --- 유틸리티 메서드 ---
    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}