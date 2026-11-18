package com.hostel.mess.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a monthly mess billing statement for a student.
 * 
 * Enforces uniqueness on (user_id, billing_month) so that each resident receives
 * only one generated bill per calendar month (e.g. '2026-11').
 */
@Entity
@Table(
    name = "billing_ledgers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_billing_month",
            columnNames = {"user_id", "billing_month"}
        )
    }
)
public class BillingLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Long billId;

    // Many BillingLedger records belong to one User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bill_user")
    )
    private User user;

    // Format: 'YYYY-MM' (e.g., '2026-11')
    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "eligible_rebate_units", nullable = false)
    private Integer eligibleRebateUnits;

    @Column(name = "deduction_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal deductionAmount;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    // Default constructor (required by JPA)
    public BillingLedger() {
    }

    // Parameterized constructor
    public BillingLedger(User user, String billingMonth, BigDecimal baseFee, Integer eligibleRebateUnits,
                         BigDecimal deductionAmount, BigDecimal netAmount, PaymentStatus paymentStatus) {
        this.user = user;
        this.billingMonth = billingMonth;
        this.baseFee = baseFee;
        this.eligibleRebateUnits = eligibleRebateUnits;
        this.deductionAmount = deductionAmount;
        this.netAmount = netAmount;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus : PaymentStatus.UNPAID;
        this.generatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.UNPAID;
        }
    }

    // Getters and Setters
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public Integer getEligibleRebateUnits() {
        return eligibleRebateUnits;
    }

    public void setEligibleRebateUnits(Integer eligibleRebateUnits) {
        this.eligibleRebateUnits = eligibleRebateUnits;
    }

    public BigDecimal getDeductionAmount() {
        return deductionAmount;
    }

    public void setDeductionAmount(BigDecimal deductionAmount) {
        this.deductionAmount = deductionAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public String toString() {
        return "BillingLedger{" +
                "billId=" + billId +
                ", billingMonth='" + billingMonth + '\'' +
                ", baseFee=" + baseFee +
                ", eligibleRebateUnits=" + eligibleRebateUnits +
                ", deductionAmount=" + deductionAmount +
                ", netAmount=" + netAmount +
                ", paymentStatus=" + paymentStatus +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
