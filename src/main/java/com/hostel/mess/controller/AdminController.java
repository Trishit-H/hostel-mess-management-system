package com.hostel.mess.controller;

import com.hostel.mess.entity.MealExemption;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.MealExemptionService;
import com.hostel.mess.service.StudentService;
import com.hostel.mess.service.dto.AttendanceSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Controller managing administrative operations:
 * - Admin Dashboard metrics & attendance overview
 * - Student account management (Add, View, Edit, Activate/Deactivate)
 * - Exemption history across hostel residents
 * - Batch monthly bill generation & payment settlement toggles
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final StudentService studentService;
    private final MealExemptionService mealExemptionService;
    private final BillingService billingService;

    public AdminController(UserRepository userRepository,
                           StudentService studentService,
                           MealExemptionService mealExemptionService,
                           BillingService billingService) {
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.mealExemptionService = mealExemptionService;
        this.billingService = billingService;
    }

    // =========================================================================
    // 1. Admin Dashboard
    // =========================================================================

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            model.addAttribute("currentUser", user);
        }

        long activeStudents = studentService.countActiveStudents();
        long totalStudents = studentService.countTotalStudents();
        AttendanceSummary todayAttendance = mealExemptionService.getExpectedAttendance(LocalDate.now());
        List<MealExemption> recentExemptions = mealExemptionService.getAllRecentExemptions();
        int totalBills = billingService.getAllBills().size();

        model.addAttribute("activeStudents", activeStudents);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("recentExemptions", recentExemptions);
        model.addAttribute("totalBills", totalBills);
        model.addAttribute("todayDate", LocalDate.now());
        model.addAttribute("currentMonth", YearMonth.now().toString());

        return "admin/dashboard";
    }

    // =========================================================================
    // 2. Student Management
    // =========================================================================

    @GetMapping("/students")
    public String listStudents(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        return "admin/students";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("student", new User());
        return "admin/student-form";
    }

    @PostMapping("/students/save")
    public String saveStudent(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String fullName,
                              @RequestParam(required = false) String roomNo,
                              RedirectAttributes redirectAttributes) {
        try {
            studentService.createStudent(username, password, fullName, roomNo);
            redirectAttributes.addFlashAttribute("successMessage", "Student '" + fullName + "' added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/students/new";
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/edit/{id}")
    public String editStudentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User student = studentService.getStudentById(id);
            model.addAttribute("student", student);
            return "admin/student-edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/students";
        }
    }

    @PostMapping("/students/update/{id}")
    public String updateStudent(@PathVariable Long id,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String roomNo,
                                @RequestParam(required = false) String password,
                                RedirectAttributes redirectAttributes) {
        try {
            studentService.updateStudent(id, fullName, roomNo, password);
            redirectAttributes.addFlashAttribute("successMessage", "Student details updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/students/edit/" + id;
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/toggle-status/{id}")
    public String toggleStudentStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.toggleStudentStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student account status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    // =========================================================================
    // 3. Exemption Overview
    // =========================================================================

    @GetMapping("/exemptions")
    public String allExemptions(Model model) {
        model.addAttribute("exemptions", mealExemptionService.getAllExemptions());
        return "admin/exemptions";
    }

    // =========================================================================
    // 4. Monthly Billing Management
    // =========================================================================

    @GetMapping("/billing")
    public String billingList(@RequestParam(required = false) String month, Model model) {
        String selectedMonth = (month != null && !month.isBlank()) ? month : YearMonth.now().toString();
        model.addAttribute("bills", billingService.getBillsByMonth(selectedMonth));
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("currentMonth", YearMonth.now().toString());
        model.addAttribute("baseFee", billingService.getBaseMonthlyFee());
        model.addAttribute("rebateRate", billingService.getRebatePerMeal());
        return "admin/billing";
    }

    @PostMapping("/billing/generate")
    public String generateBills(@RequestParam String billingMonth, RedirectAttributes redirectAttributes) {
        try {
            int[] result = billingService.generateBillsForAllActiveStudents(billingMonth);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Billing process complete for " + billingMonth + ": " +
                    result[0] + " bill(s) generated successfully, " +
                    result[1] + " resident(s) already billed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/billing?month=" + billingMonth;
    }

    @PostMapping("/billing/toggle-status/{id}")
    public String togglePaymentStatus(@PathVariable Long id,
                                      @RequestParam(required = false) String returnMonth,
                                      RedirectAttributes redirectAttributes) {
        try {
            billingService.togglePaymentStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/billing?month=" + (returnMonth != null ? returnMonth : YearMonth.now().toString());
    }
}
