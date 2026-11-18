package com.hostel.mess.service;

import com.hostel.mess.entity.BillingLedger;
import com.hostel.mess.entity.PaymentStatus;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.BillingLedgerRepository;
import com.hostel.mess.service.dto.RebateSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing monthly billing operations, rebate deductions, and payment status updates.
 */
@Service
@Transactional
public class BillingService {

    private final BillingLedgerRepository billingLedgerRepository;
    private final MealExemptionService mealExemptionService;
    private final StudentService studentService;

    @Value("${app.billing.base-monthly-fee:3000.00}")
    private BigDecimal baseMonthlyFee;

    @Value("${app.billing.rebate-per-meal:40.00}")
    private BigDecimal rebatePerMeal;

    public BillingService(BillingLedgerRepository billingLedgerRepository,
                          MealExemptionService mealExemptionService,
                          StudentService studentService) {
        this.billingLedgerRepository = billingLedgerRepository;
        this.mealExemptionService = mealExemptionService;
        this.studentService = studentService;
    }

    public BigDecimal getBaseMonthlyFee() {
        return baseMonthlyFee;
    }

    public BigDecimal getRebatePerMeal() {
        return rebatePerMeal;
    }

    /**
     * Calculates the rebate summary for a resident for a given billing month (YYYY-MM).
     */
    @Transactional(readOnly = true)
    public RebateSummary calculateRebateForMonth(User user, String billingMonth) {
        YearMonth ym = parseBillingMonth(billingMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        long eligibleCount = mealExemptionService.countApprovedExemptionsInMonth(user, start, end);
        return new RebateSummary(billingMonth, (int) eligibleCount, rebatePerMeal, baseMonthlyFee);
    }

    /**
     * Generates a monthly bill for a single resident.
     * Prevents duplicate bills for the same student and month.
     */
    public BillingLedger generateBillForStudent(User user, String billingMonth) {
        if (billingLedgerRepository.existsByUserAndBillingMonth(user, billingMonth)) {
            throw new IllegalArgumentException(
                    "A bill for student " + user.getFullName() + " for month " + billingMonth + " already exists."
            );
        }

        RebateSummary summary = calculateRebateForMonth(user, billingMonth);

        BillingLedger ledger = new BillingLedger(
                user,
                billingMonth,
                summary.getBaseMonthlyFee(),
                summary.getEligibleRebateUnits(),
                summary.getTotalDeduction(),
                summary.getNetPayable(),
                PaymentStatus.UNPAID
        );

        return billingLedgerRepository.save(ledger);
    }

    /**
     * Generates bills for all currently active students for the specified month (YYYY-MM).
     * Skips students who already have a bill for that month.
     * 
     * @return Array of [generatedCount, skippedCount]
     */
    public int[] generateBillsForAllActiveStudents(String billingMonth) {
        // Validate month format
        parseBillingMonth(billingMonth);

        List<User> activeStudents = studentService.getActiveStudents();
        int generated = 0;
        int skipped = 0;

        for (User student : activeStudents) {
            if (billingLedgerRepository.existsByUserAndBillingMonth(student, billingMonth)) {
                skipped++;
            } else {
                RebateSummary summary = calculateRebateForMonth(student, billingMonth);
                BillingLedger ledger = new BillingLedger(
                        student,
                        billingMonth,
                        summary.getBaseMonthlyFee(),
                        summary.getEligibleRebateUnits(),
                        summary.getTotalDeduction(),
                        summary.getNetPayable(),
                        PaymentStatus.UNPAID
                );
                billingLedgerRepository.save(ledger);
                generated++;
            }
        }

        return new int[]{generated, skipped};
    }

    /**
     * Toggles a bill's settlement status between PAID and UNPAID.
     */
    public BillingLedger togglePaymentStatus(Long billId) {
        BillingLedger bill = billingLedgerRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Billing statement not found with ID: " + billId));

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            bill.setPaymentStatus(PaymentStatus.UNPAID);
        } else {
            bill.setPaymentStatus(PaymentStatus.PAID);
        }

        return billingLedgerRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public List<BillingLedger> getBillsForUser(User user) {
        return billingLedgerRepository.findByUserOrderByBillingMonthDesc(user);
    }

    @Transactional(readOnly = true)
    public Optional<BillingLedger> getLatestBillForUser(User user) {
        return billingLedgerRepository.findTop1ByUserOrderByBillingMonthDesc(user);
    }

    @Transactional(readOnly = true)
    public List<BillingLedger> getAllBills() {
        return billingLedgerRepository.findAllByOrderByBillingMonthDescGeneratedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<BillingLedger> getBillsByMonth(String billingMonth) {
        if (billingMonth != null && !billingMonth.isBlank()) {
            return billingLedgerRepository.findByBillingMonthOrderByUserFullNameAsc(billingMonth);
        }
        return getAllBills();
    }

    private YearMonth parseBillingMonth(String billingMonth) {
        if (billingMonth == null || billingMonth.isBlank()) {
            throw new IllegalArgumentException("Billing month is required in 'YYYY-MM' format (e.g. 2026-11).");
        }
        try {
            return YearMonth.parse(billingMonth.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid billing month format: '" + billingMonth + "'. Expected 'YYYY-MM' (e.g. 2026-11).");
        }
    }
}
