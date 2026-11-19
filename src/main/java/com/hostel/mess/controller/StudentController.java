package com.hostel.mess.controller;


import com.hostel.mess.entity.MealExemption;
import com.hostel.mess.entity.MealSlot;
import com.hostel.mess.entity.User;

import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.MealExemptionService;
import com.hostel.mess.service.dto.RebateSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.entity.BillingLedger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Controller managing Student self-service operations:
 * - Student Dashboard (Profile, live rebate overview, upcoming exemptions, quick apply form)
 * - Advance Meal Exemption submission and cutoff-validated cancellations
 * - Monthly Mess Invoices and itemized rebate deduction statements
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

        // Ensure the latest bill for the current month exists and is up‑to‑date
        billingService.syncUnpaidBillWithLiveRebate(user, currentMonth);
        Optional<BillingLedger> existingBill = billingService.getLatestBillForUser(user);
        if (existingBill.isEmpty() || !existingBill.get().getBillingMonth().equals(currentMonth)) {
            // Generate a new bill for the current month if none exists
            billingService.generateBillForStudent(user, currentMonth);
        }

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

    @GetMapping("/pay/{billId}")
    public String showPayPage(@PathVariable Long billId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = getAuthenticatedUser(userDetails);
        BillingLedger bill = billingService.getBillById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        // Ensure the bill belongs to the logged‑in student
        if (!bill.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("You cannot pay a bill that is not yours.");
        }
        model.addAttribute("currentUser", user);
        model.addAttribute("bill", bill);
        return "student/pay";
    }

    @PostMapping("/pay/{billId}/confirm")
    public String confirmPay(@PathVariable Long billId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        BillingLedger bill = billingService.getBillById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        if (!bill.getUser().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot pay a bill that is not yours.");
            return "redirect:/student/dashboard";
        }
        if (bill.getPaymentStatus() == com.hostel.mess.entity.PaymentStatus.PAID) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bill is already marked as PAID.");
            return "redirect:/student/dashboard";
        }
        billingService.markBillAsPaid(billId);
        redirectAttributes.addFlashAttribute("successMessage", "Bill for " + bill.getBillingMonth() + " has been marked as PAID.");
        return "redirect:/student/dashboard";
    }

    @GetMapping("/bills")
    public String viewBills(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        String currentMonth = YearMonth.now().toString();

        billingService.syncUnpaidBillWithLiveRebate(user, currentMonth);

        model.addAttribute("currentUser", user);
        model.addAttribute("bills", billingService.getBillsForUser(user));
        model.addAttribute("currentRebate", billingService.calculateRebateForMonth(user, currentMonth));
        return "student/bills";
    }
}
