package com.hostel.mess.controller;

import com.hostel.mess.entity.BillingLedger;
import com.hostel.mess.entity.MealExemption;
import com.hostel.mess.entity.MealSlot;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.MealExemptionService;
import com.hostel.mess.service.dto.RebateSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Controller managing student resident interactions:
 * - Student Portal Dashboard
 * - Advance meal exemption submissions & cancellations
 * - Exemption history tracking
 * - Monthly mess bills & rebate transparency
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final UserRepository userRepository;
    private final MealExemptionService mealExemptionService;
    private final BillingService billingService;

    public StudentController(UserRepository userRepository,
                             MealExemptionService mealExemptionService,
                             BillingService billingService) {
        this.userRepository = userRepository;
        this.mealExemptionService = mealExemptionService;
        this.billingService = billingService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Authentication session expired.");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User account not found: " + userDetails.getUsername()));
    }

    // =========================================================================
    // 1. Student Portal Dashboard
    // =========================================================================

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        String currentMonth = YearMonth.now().toString();

        List<MealExemption> recentExemptions = mealExemptionService.getRecentExemptions(user);
        RebateSummary currentRebate = billingService.calculateRebateForMonth(user, currentMonth);
        Optional<BillingLedger> currentBill = billingService.getLatestBillForUser(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("recentExemptions", recentExemptions);
        model.addAttribute("currentRebate", currentRebate);
        model.addAttribute("currentBill", currentBill.orElse(null));
        model.addAttribute("mealSlots", MealSlot.values());
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusMonths(1));

        return "student/dashboard";
    }

    // =========================================================================
    // 2. Meal Exemption Actions
    // =========================================================================

    @GetMapping("/exemptions")
    public String viewExemptions(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("currentUser", user);
        model.addAttribute("exemptions", mealExemptionService.getExemptionHistory(user));
        model.addAttribute("mealSlots", MealSlot.values());
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusMonths(1));
        return "student/exemptions";
    }

    @PostMapping("/exemptions/submit")
    public String submitExemption(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
                                  @RequestParam MealSlot mealSlot,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            mealExemptionService.submitExemption(user, targetDate, mealSlot);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Exemption submitted successfully for " + mealSlot + " on " + targetDate + " (Automatically APPROVED).");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/dashboard";
    }

    @PostMapping("/exemptions/cancel/{id}")
    public String cancelExemption(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id,
                                  @RequestParam(defaultValue = "dashboard") String source,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            mealExemptionService.cancelExemption(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Exemption cancelled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + ("exemptions".equals(source) ? "/student/exemptions" : "/student/dashboard");
    }

    // =========================================================================
    // 3. Monthly Mess Bills
    // =========================================================================

    @GetMapping("/bills")
    public String viewBills(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        String currentMonth = YearMonth.now().toString();

        model.addAttribute("currentUser", user);
        model.addAttribute("bills", billingService.getBillsForUser(user));
        model.addAttribute("currentRebate", billingService.calculateRebateForMonth(user, currentMonth));
        return "student/bills";
    }
}
