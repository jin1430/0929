package com.example.GoCafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class IntroController {

    @GetMapping("/")
    public String intro(Model model, Authentication auth) {
        boolean isLoggedIn = (auth != null && auth.isAuthenticated());
        model.addAttribute("isLoggedIn", isLoggedIn);

        // 임시 통계 (실서비스는 서비스/리포지토리에서 조회)
        model.addAttribute("reviewerCount", 128);
        model.addAttribute("missionCount", 9);
        model.addAttribute("cafesIndexed", 433);

        return "page/intro";
    }
}
