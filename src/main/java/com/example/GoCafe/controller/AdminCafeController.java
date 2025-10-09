// src/main/java/com/example/GoCafe/controller/AdminCafeController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/cafes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCafeController {

    private final CafeService cafeService;

    @PostMapping("/{cafeId}/approve")
    public String approve(@PathVariable Long cafeId) {
        // 기존 코드의 updateStatus는 없으므로 실제 존재하는 메서드로 교체
        cafeService.approve(cafeId);
        return "redirect:/admin#tab-cafes";
    }

    @PostMapping("/{cafeId}/reject")
    public String reject(@PathVariable Long cafeId) {
        cafeService.reject(cafeId);
        return "redirect:/admin#tab-cafes";
    }

    /**
     * 관리자 페이지에서 증빙서 다운로드
     * - cafe.getBizDoc() 이 "/uploads/..." 면 파일 내용을 스트리밍
     * - 파일명은 URL의 파일명만 추출해서 Content-Disposition에 세팅
     */
    @GetMapping("/{cafeId}/bizdoc")
    public ResponseEntity<ByteArrayResource> downloadBizDoc(@PathVariable Long cafeId) {
        Cafe cafe = cafeService.findById(cafeId);
        if (cafe == null) return ResponseEntity.notFound().build();

        String url = cafe.getBizDoc();
        if (url == null || url.isBlank()) return ResponseEntity.notFound().build();

        // 파일 바이트 로드 (FileStorageService.loadAsBytes를 CafeService에서 래핑)
        byte[] bytes = cafeService.loadBizDocBytes(cafeId);

        // 다운로드 파일명: URL의 베이스네임만 사용
        String fileName = Paths.get(url).getFileName().toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
