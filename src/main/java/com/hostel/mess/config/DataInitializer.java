package com.hostel.mess.config;

import com.hostel.mess.entity.*;
import com.hostel.mess.repository.BillingLedgerRepository;
import com.hostel.mess.repository.MealExemptionRepository;
import com.hostel.mess.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds comprehensive, realistic production-grade demonstration data on startup:
 * - Administrator and Mess Staff accounts
 * - 15 Active Students across various hostel wings and rooms
 * - 1 Deactivated Student account for testing status checks
 * - Dozens of Meal Exemptions across past, present, and future dates (Breakfast, Lunch, Dinner)
 * - Monthly Billing Ledgers for previous and current calendar months with realistic rebates and payment statuses.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final MealExemptionRepository mealExemptionRepository;
    private final BillingLedgerRepository billingLedgerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin.username:admin}")
    private String adminUsername;

    @Value("${app.init.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.init.admin.full-name:System Administrator}")
    private String adminFullName;

    @Value("${app.billing.base-monthly-fee:3000.00}")
    private BigDecimal baseMonthlyFee;

    @Value("${app.billing.rebate-per-meal:40.00}")
    private BigDecimal rebatePerMeal;

    public DataInitializer(UserRepository userRepository,
                           MealExemptionRepository mealExemptionRepository,
                           BillingLedgerRepository billingLedgerRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mealExemptionRepository = mealExemptionRepository;
        this.billingLedgerRepository = billingLedgerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting demonstration dataset initialization...");

        // 1. Seed Administrator
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    adminFullName,
                    "ADMIN-OFFICE",
                    Role.ADMIN,
                    true
            );
            userRepository.save(admin);
            log.info("Initialized default Administrator: [username: {}]", adminUsername);
        }

        // 2. Seed Mess Staff
        seedMessStaff("staff1", "Ramesh Kumar (Mess Manager)", "MESS-OFFICE");
        seedMessStaff("staff2", "Suresh Rao (Kitchen Supervisor)", "KITCHEN-COUNTER-1");

        // 3. Seed Students (15 Active + 1 Inactive)
        List<User> students = new ArrayList<>();
        students.add(seedStudent("student1", "Rahul Sharma", "B-204", true));
        students.add(seedStudent("student2", "Priya Patel", "A-101", true));
        students.add(seedStudent("student3", "Rohan Gupta", "C-305", true));
        students.add(seedStudent("student4", "Sneha Mukherjee", "A-202", true));
        students.add(seedStudent("student5", "Vikram Aditya", "B-110", true));
        students.add(seedStudent("student6", "Ananya Roy", "D-104", true));
        students.add(seedStudent("student7", "Arjun Nair", "C-212", true));
        students.add(seedStudent("student8", "Kavita Reddy", "A-308", true));
        students.add(seedStudent("student9", "Deep Roy", "B-301", true));
        students.add(seedStudent("student10", "Meera Iyer", "D-205", true));
        students.add(seedStudent("student11", "Tanmay Deshmukh", "C-107", true));
        students.add(seedStudent("student12", "Pooja Banerjee", "A-115", true));
        students.add(seedStudent("student13", "Saurabh Joshi", "B-220", true));
        students.add(seedStudent("student14", "Riya Sen", "D-310", true));
        students.add(seedStudent("student15", "Nikhil Verma", "C-402", true));
        seedStudent("inactive_student", "Amit Verma (On Leave)", "A-102", false);

        // 4. Seed Meal Exemptions (Today, Tomorrow, Future, and Past dates)
        seedMealExemptions(students);

        // 5. Seed Monthly Billing Ledgers (Previous Month & Current Month)
        seedBillingLedgers(students);

        log.info("Demonstration dataset initialized successfully.");
    }

    private User seedStudent(String username, String fullName, String roomNo, boolean active) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User s = new User(
                    username,
                    passwordEncoder.encode("Student@123"),
                    fullName,
                    roomNo,
                    Role.STUDENT,
                    active
            );
            return userRepository.save(s);
        });
    }

    private void seedMessStaff(String username, String fullName, String roomNo) {
        if (!userRepository.existsByUsername(username)) {
            User staff = new User(
                    username,
                    passwordEncoder.encode("Staff@123"),
                    fullName,
                    roomNo,
                    Role.MESS_STAFF,
                    true
            );
            userRepository.save(staff);
            log.info("Initialized Mess Staff: [username: {}]", username);
        }
    }

    private void seedMealExemptions(List<User> students) {
        if (students.isEmpty()) return;

        LocalDate today = LocalDate.now();

        // Today's Exemptions (Affects Kitchen Attendance live display)
        saveExemption(students.get(0), today, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(0), today, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(1), today, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(1), today, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(2), today, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(6), today, MealSlot.BREAKFAST, ExemptionStatus.CANCELLED);
        saveExemption(students.get(7), today, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(11), today, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(13), today, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        // Tomorrow's Exemptions
        LocalDate tomorrow = today.plusDays(1);
        saveExemption(students.get(0), tomorrow, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(0), tomorrow, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(0), tomorrow, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(3), tomorrow, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(5), tomorrow, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(8), tomorrow, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(8), tomorrow, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(10), tomorrow, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(12), tomorrow, MealSlot.DINNER, ExemptionStatus.APPROVED);

        // Day after Tomorrow's Exemptions
        LocalDate dayAfter = today.plusDays(2);
        saveExemption(students.get(1), dayAfter, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(2), dayAfter, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(2), dayAfter, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), dayAfter, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(9), dayAfter, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(14), dayAfter, MealSlot.DINNER, ExemptionStatus.APPROVED);

        // Future Dates (+3 and +4 days)
        LocalDate day3 = today.plusDays(3);
        saveExemption(students.get(0), day3, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(0), day3, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(6), day3, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(7), day3, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        LocalDate day4 = today.plusDays(4);
        saveExemption(students.get(3), day4, MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(5), day4, MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(11), day4, MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        // Past Dates (Giving a full rich history of exemptions)
        saveExemption(students.get(0), today.minusDays(1), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(1), today.minusDays(1), MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(2), today.minusDays(1), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(8), today.minusDays(1), MealSlot.LUNCH, ExemptionStatus.APPROVED);

        saveExemption(students.get(0), today.minusDays(2), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(0), today.minusDays(2), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today.minusDays(2), MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(6), today.minusDays(2), MealSlot.BREAKFAST, ExemptionStatus.CANCELLED);

        saveExemption(students.get(2), today.minusDays(3), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(3), today.minusDays(3), MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(12), today.minusDays(3), MealSlot.LUNCH, ExemptionStatus.APPROVED);

        saveExemption(students.get(1), today.minusDays(4), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today.minusDays(4), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(7), today.minusDays(4), MealSlot.DINNER, ExemptionStatus.APPROVED);

        saveExemption(students.get(0), today.minusDays(5), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(5), today.minusDays(5), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(10), today.minusDays(5), MealSlot.DINNER, ExemptionStatus.APPROVED);

        saveExemption(students.get(2), today.minusDays(6), MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(8), today.minusDays(6), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        saveExemption(students.get(3), today.minusDays(7), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today.minusDays(7), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today.minusDays(7), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(4), today.minusDays(7), MealSlot.DINNER, ExemptionStatus.APPROVED);

        saveExemption(students.get(0), today.minusDays(8), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(6), today.minusDays(8), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(9), today.minusDays(8), MealSlot.DINNER, ExemptionStatus.APPROVED);

        saveExemption(students.get(1), today.minusDays(9), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(7), today.minusDays(9), MealSlot.DINNER, ExemptionStatus.CANCELLED);
        saveExemption(students.get(13), today.minusDays(9), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        saveExemption(students.get(0), today.minusDays(10), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(2), today.minusDays(10), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(11), today.minusDays(10), MealSlot.DINNER, ExemptionStatus.APPROVED);

        saveExemption(students.get(4), today.minusDays(12), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(5), today.minusDays(12), MealSlot.DINNER, ExemptionStatus.APPROVED);
        saveExemption(students.get(14), today.minusDays(12), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);

        saveExemption(students.get(0), today.minusDays(14), MealSlot.LUNCH, ExemptionStatus.APPROVED);
        saveExemption(students.get(1), today.minusDays(14), MealSlot.BREAKFAST, ExemptionStatus.APPROVED);
        saveExemption(students.get(3), today.minusDays(14), MealSlot.DINNER, ExemptionStatus.APPROVED);
    }

    private void saveExemption(User user, LocalDate date, MealSlot slot, ExemptionStatus status) {
        if (!mealExemptionRepository.existsByUserAndTargetDateAndMealSlot(user, date, slot)) {
            MealExemption ex = new MealExemption(user, date, slot, status);
            mealExemptionRepository.save(ex);
        }
    }

    private void seedBillingLedgers(List<User> students) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth prevMonth = currentMonth.minusMonths(1);

        String prevMonthStr = prevMonth.toString();
        String currentMonthStr = currentMonth.toString();

        // Previous Month Data (Closed & Settled Cycle)
        int[] prevRebateUnits = {15, 10, 20, 8, 25, 5, 12, 18, 2, 14, 7, 6, 11, 4, 16};
        PaymentStatus[] prevStatuses = {
                PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.PAID,
                PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.UNPAID, PaymentStatus.PAID,
                PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.UNPAID,
                PaymentStatus.PAID, PaymentStatus.PAID, PaymentStatus.PAID
        };

        for (int i = 0; i < students.size() && i < prevRebateUnits.length; i++) {
            saveBill(students.get(i), prevMonthStr, prevRebateUnits[i], prevStatuses[i]);
        }

        // Current Month Data (Active Billing Cycle)
        int[] currRebateUnits = {9, 6, 11, 5, 14, 4, 8, 10, 3, 7, 5, 8};
        PaymentStatus[] currStatuses = {
                PaymentStatus.UNPAID, PaymentStatus.PAID, PaymentStatus.UNPAID, PaymentStatus.UNPAID,
                PaymentStatus.UNPAID, PaymentStatus.PAID, PaymentStatus.UNPAID, PaymentStatus.UNPAID,
                PaymentStatus.UNPAID, PaymentStatus.PAID, PaymentStatus.UNPAID, PaymentStatus.UNPAID
        };

        for (int i = 0; i < students.size() && i < currRebateUnits.length; i++) {
            saveBill(students.get(i), currentMonthStr, currRebateUnits[i], currStatuses[i]);
        }
    }

    private void saveBill(User user, String billingMonth, int rebateUnits, PaymentStatus paymentStatus) {
        if (!billingLedgerRepository.existsByUserAndBillingMonth(user, billingMonth)) {
            BigDecimal base = (this.baseMonthlyFee != null) ? this.baseMonthlyFee : new BigDecimal("3000.00");
            BigDecimal rate = (this.rebatePerMeal != null) ? this.rebatePerMeal : new BigDecimal("40.00");
            BigDecimal deduction = rate.multiply(BigDecimal.valueOf(rebateUnits));
            BigDecimal net = base.subtract(deduction).max(BigDecimal.ZERO);

            BillingLedger bill = new BillingLedger(
                    user,
                    billingMonth,
                    base,
                    rebateUnits,
                    deduction,
                    net,
                    paymentStatus
            );
            billingLedgerRepository.save(bill);
        }
    }
}
