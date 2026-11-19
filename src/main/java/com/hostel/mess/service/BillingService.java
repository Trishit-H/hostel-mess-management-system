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
import java.time.LocalDateTime;
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
     * Generates or recalculates a monthly bill for a single resident.
     * If an UNPAID bill exists, it recalculates with current approved exemptions.
     * If a PAID bill exists, prevents modification to preserve the payment audit trail.
     */
    public BillingLedger generateBillForStudent(User user, String billingMonth) {
        parseBillingMonth(billingMonth);

        Optional<BillingLedger> existingOpt = billingLedgerRepository.findByUserAndBillingMonth(user, billingMonth);
        if (existingOpt.isPresent()) {
            BillingLedger bill = existingOpt.get();
            if (bill.getPaymentStatus() == PaymentStatus.PAID) {
                throw new IllegalStateException(
                        "Cannot modify bill for student " + user.getFullName() + " for month " + billingMonth + " because it is already marked as PAID."
                );
            }
            // Recalculate UNPAID bill
            RebateSummary summary = calculateRebateForMonth(user, billingMonth);
            bill.setBaseFee(summary.getBaseMonthlyFee());
            bill.setEligibleRebateUnits(summary.getEligibleRebateUnits());
            bill.setDeductionAmount(summary.getTotalDeduction());
            bill.setNetAmount(summary.getNetPayable());
            bill.setGeneratedAt(LocalDateTime.now());
            return billingLedgerRepository.save(bill);
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
     * Generates or recalculates bills for all currently active students for the specified month (YYYY-MM).
     * Unpaid bills are safely updated to reflect the latest approved exemptions.
     * Paid bills are strictly preserved and skipped.
     * 
     * @return Array of [processedCount, skippedPaidCount]
     */
    public int[] generateBillsForAllActiveStudents(String billingMonth) {
        parseBillingMonth(billingMonth);

        List<User> activeStudents = studentService.getActiveStudents();
        int processed = 0;
        int skipped = 0;

        for (User student : activeStudents) {
            Optional<BillingLedger> existingOpt = billingLedgerRepository.findByUserAndBillingMonth(student, billingMonth);
            if (existingOpt.isPresent()) {
                BillingLedger bill = existingOpt.get();
                if (bill.getPaymentStatus() == PaymentStatus.PAID) {
                    skipped++;
                } else {
                    RebateSummary summary = calculateRebateForMonth(student, billingMonth);
                    bill.setBaseFee(summary.getBaseMonthlyFee());
                    bill.setEligibleRebateUnits(summary.getEligibleRebateUnits());
                    bill.setDeductionAmount(summary.getTotalDeduction());
                    bill.setNetAmount(summary.getNetPayable());
                    bill.setGeneratedAt(LocalDateTime.now());
                    billingLedgerRepository.save(bill);
                    processed++;
                }
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
                processed++;
            }
        }

        return new int[]{processed, skipped};
    }

    /**
     * Synchronizes an existing UNPAID bill for the given month with the latest live approved exemptions.
     * Does nothing if no bill exists or if the bill is already PAID.
     */
    public void syncUnpaidBillWithLiveRebate(User user, String billingMonth) {
        if (user == null || billingMonth == null) return;
        try {
            Optional<BillingLedger> billOpt = billingLedgerRepository.findByUserAndBillingMonth(user, billingMonth);
            if (billOpt.isPresent()) {
                BillingLedger bill = billOpt.get();
                if (bill.getPaymentStatus() == PaymentStatus.UNPAID) {
                    RebateSummary summary = calculateRebateForMonth(user, billingMonth);
                    if (!bill.getEligibleRebateUnits().equals(summary.getEligibleRebateUnits())
                            || bill.getNetAmount().compareTo(summary.getNetPayable()) != 0) {
                        bill.setEligibleRebateUnits(summary.getEligibleRebateUnits());
                        bill.setDeductionAmount(summary.getTotalDeduction());
                        bill.setNetAmount(summary.getNetPayable());
                        billingLedgerRepository.save(bill);
                    }
                }
            }
        } catch (Exception ignored) {
        }
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
    public Optional<BillingLedger> getBillById(Long billId) {
        return billingLedgerRepository.findById(billId);
    }


    /**
     * Marks the given bill as PAID. Used by the student “Pay” flow.
     */

    @Transactional
    public BillingLedger markBillAsPaid(Long billId) {
        BillingLedger bill = billingLedgerRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Billing statement not found with ID: " + billId));

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Bill is already marked as PAID.");
        }

        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.setGeneratedAt(LocalDateTime.now());

        return billingLedgerRepository.save(bill);
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
