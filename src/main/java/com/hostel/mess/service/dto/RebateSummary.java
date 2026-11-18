package com.hostel.mess.service.dto;

import java.math.BigDecimal;

/**
 * Data transfer object encapsulating meal rebate calculations for a specific calendar month.
 * 
 * Formula:
 * - Deduction Amount = Eligible Rebate Units * Rebate Per Meal
 * - Net Payable = max(0, Base Monthly Fee - Deduction Amount)
 */
public class RebateSummary {

    private String billingMonth;
    private int eligibleRebateUnits;
    private BigDecimal rebatePerMeal;
    private BigDecimal totalDeduction;
    private BigDecimal baseMonthlyFee;
    private BigDecimal netPayable;

    public RebateSummary() {
    }

    public RebateSummary(String billingMonth, int eligibleRebateUnits, BigDecimal rebatePerMeal,
                         BigDecimal baseMonthlyFee) {
        this.billingMonth = billingMonth;
        this.eligibleRebateUnits = eligibleRebateUnits;
        this.rebatePerMeal = rebatePerMeal;
        this.baseMonthlyFee = baseMonthlyFee;
        this.totalDeduction = rebatePerMeal.multiply(BigDecimal.valueOf(eligibleRebateUnits));
        this.netPayable = baseMonthlyFee.subtract(this.totalDeduction).max(BigDecimal.ZERO);
    }

    // Getters and Setters
    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
        this.billingMonth = billingMonth;
    }

    public int getEligibleRebateUnits() {
        return eligibleRebateUnits;
    }

    public void setEligibleRebateUnits(int eligibleRebateUnits) {
        this.eligibleRebateUnits = eligibleRebateUnits;
    }

    public BigDecimal getRebatePerMeal() {
        return rebatePerMeal;
    }

    public void setRebatePerMeal(BigDecimal rebatePerMeal) {
        this.rebatePerMeal = rebatePerMeal;
    }

    public BigDecimal getTotalDeduction() {
        return totalDeduction;
    }

    public void setTotalDeduction(BigDecimal totalDeduction) {
        this.totalDeduction = totalDeduction;
    }

    public BigDecimal getBaseMonthlyFee() {
        return baseMonthlyFee;
    }

    public void setBaseMonthlyFee(BigDecimal baseMonthlyFee) {
        this.baseMonthlyFee = baseMonthlyFee;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable;
    }
}
