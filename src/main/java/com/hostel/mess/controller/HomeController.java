package com.hostel.mess.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for application entry point navigation.
 * Redirects authenticated users directly to their role-specific dashboard,
 * or unauthenticated users directly to the login page.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    return "redirect:/admin/dashboard";
                } else if ("ROLE_STUDENT".equals(role)) {
                    return "redirect:/student/dashboard";
                } else if ("ROLE_MESS_STAFF".equals(role)) {
                    return "redirect:/mess/dashboard";
                }
            }
        }
        return "redirect:/login";
    }
}
