package com.hostel.mess.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for public and landing page navigation.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("title", "Automated Hostel Mess Meal Exemption & Billing System");
        model.addAttribute("status", "System Online - Initial Setup Completed");
        return "index";
    }
}
