package com.example.GoCafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class IntroController {

    @GetMapping("/")
    public String intro(Model model, Authentication auth) {
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());
        model.addAttribute("reviewerCount", 50);      // Long/Integer/String 모두 OK
        model.addAttribute("cafesIndexed", "1,200+");
        model.addAttribute("totalReviews", "15,000+");
        model.addAttribute("satisfactionRate", "98%");
        return "page/intro";
    }

}
