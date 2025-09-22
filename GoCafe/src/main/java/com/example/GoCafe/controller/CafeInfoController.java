package com.example.GoCafe.controller;

import com.example.GoCafe.dto.CafeInfoForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeInfo;
import com.example.GoCafe.service.CafeInfoService;
import com.example.GoCafe.support.EntityIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/owner/cafes")
public class CafeInfoController {

    private final CafeInfoService cafeInfoService;

    /** 업서트: 기존 있으면 수정, 없으면 생성 */
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info")
    public String upsert(@PathVariable Long cafeId,
                         @ModelAttribute CafeInfoForm form,
                         RedirectAttributes ra) {

        // 폼 → 엔티티
        CafeInfo entity = form.toEntity();

        // 연관관계: PK만 세팅한 프록시 객체
        Cafe cafeRef = new Cafe();
        EntityIdUtil.setId(cafeRef, cafeId);
        entity.setCafe(cafeRef);

        // 길이 제한 안전 처리 (notice=20, info=500, open/close/holiday=7)
        entity.setNotice(trim(form.getCafeNotice(), 20));
        entity.setInfo(trim(form.getCafeInfo(), 500));
        entity.setOpenTime(trim(form.getCafeOpenTime(), 7));
        entity.setCloseTime(trim(form.getCafeCloseTime(), 7));
        entity.setHoliday(trim(form.getCafeHoliday(), 7));

        cafeInfoService.upsertByCafeId(cafeId, entity);

        ra.addFlashAttribute("success", "카페 정보가 저장되었습니다.");
        return "redirect:/cafes/" + cafeId;
    }

    // create/update를 별도로 쓰고 싶으면 아래도 upsert로 위임
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info/create")
    public String create(@PathVariable Long cafeId,
                         @ModelAttribute CafeInfoForm form,
                         RedirectAttributes ra) {
        return upsert(cafeId, form, ra);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info/update")
    public String update(@PathVariable Long cafeId,
                         @ModelAttribute CafeInfoForm form,
                         RedirectAttributes ra) {
        return upsert(cafeId, form, ra);
    }

    // --- util ---
    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
