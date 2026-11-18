package com.hostel.mess.controller;

import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.MealExemptionService;
import com.hostel.mess.service.dto.AttendanceSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller for Mess Staff operational management.
 * 
 * Provides expected dining attendance figures for Breakfast, Lunch, and Dinner
 * calculated as: Expected Diners = Active Students - Approved Exemptions.
 */
@Controller
@RequestMapping("/mess")
public class MessStaffController {

    private final UserRepository userRepository;
    private final MealExemptionService mealExemptionService;

    public MessStaffController(UserRepository userRepository,
                               MealExemptionService mealExemptionService) {
        this.userRepository = userRepository;
        this.mealExemptionService = mealExemptionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            model.addAttribute("currentUser", user);
        }

        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        AttendanceSummary attendanceSummary = mealExemptionService.getExpectedAttendance(selectedDate);

        model.addAttribute("attendance", attendanceSummary);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("todayDate", LocalDate.now());

        return "mess/dashboard";
    }
}
