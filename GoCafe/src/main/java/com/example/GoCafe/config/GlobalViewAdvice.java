package com.example.GoCafe.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalViewAdvice {

    @ModelAttribute
    public void injectAuthInfo(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isLoggedIn", isLoggedIn);

        String email = null;
        String nickname = null;

        if (isLoggedIn) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof String s) {
                email = s;
            }
            if (email != null) {
                int at = email.indexOf('@');
                nickname = at > 0 ? email.substring(0, at) : email;
            }
        }

        // ✅ 관리자 여부 주입
        boolean isAdmin = isLoggedIn && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        model.addAttribute("isAdmin", isAdmin);

        model.addAttribute("currentUserEmail", email);
        model.addAttribute("currentUserNickname", nickname);
    }
}
