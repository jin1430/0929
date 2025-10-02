package com.example.GoCafe.controller;

// ... (imports)

import com.example.GoCafe.service.CafeInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/owner/cafes")
@RequiredArgsConstructor
public class CafeInfoController {

    private final CafeInfoService cafeInfoService;

    /*
     * 아래의 모든 메서드는 OwnerController로 기능이 이전되었으므로 삭제합니다.
     * 클래스 자체를 비워두거나, 다른 기능이 없다면 파일 자체를 삭제해도 무방합니다.
     */

    /*
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info")
    public String upsert(...) { ... }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info/create")
    public String create(...) { ... }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{cafeId}/info/update")
    public String update(...) { ... }

    private static String trim(String s, int max) { ... }
    */
}