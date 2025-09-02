package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/cafes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCafeController {

    private final CafeService cafeService;

    @GetMapping("/pending")
    public List<Cafe> pending() {
        return cafeService.findByStatus(CafeStatus.PENDING);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) { cafeService.approve(id); }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) { cafeService.reject(id); }
}
