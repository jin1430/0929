// src/main/java/com/example/GoCafe/controller/AdminPageController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller                 // ✅ 여기!! (RestController 아님)
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final CafeService cafeService;

    @GetMapping
    public String main(Model model) {
        model.addAttribute("pendingCafes", cafeService.findByStatus(CafeStatus.PENDING));
        return "admin/main"; // ✅ 파일명과 정확히 맞추기 (templates/admin/index.mustache)
    }
}
