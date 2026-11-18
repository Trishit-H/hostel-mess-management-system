package com.hostel.mess.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an advance meal exemption (opt-out) submitted by a student.
 * 
 * Enforces uniqueness on (user_id, target_date, meal_slot) to prevent duplicate
 * exemption entries for the same student on the same meal.
 */
@Entity
@Table(
    name = "meal_exemptions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_date_meal",
            columnNames = {"user_id", "target_date", "meal_slot"}
        )
    }
)
public class MealExemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exemption_id")
    private Long exemptionId;

    // Many MealExemption records belong to one User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_exemption_user")
    )
    private User user;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_slot", nullable = false, length = 20)
    private MealSlot mealSlot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExemptionStatus status = ExemptionStatus.APPROVED;

    // Default constructor (required by JPA)
    public MealExemption() {
    }

    // Parameterized constructor
    public MealExemption(User user, LocalDate targetDate, MealSlot mealSlot, ExemptionStatus status) {
        this.user = user;
        this.targetDate = targetDate;
        this.mealSlot = mealSlot;
        this.status = (status != null) ? status : ExemptionStatus.APPROVED;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ExemptionStatus.APPROVED;
        }
    }

    // Getters and Setters
    public Long getExemptionId() {
        return exemptionId;
    }

    public void setExemptionId(Long exemptionId) {
        this.exemptionId = exemptionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public void setMealSlot(MealSlot mealSlot) {
        this.mealSlot = mealSlot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ExemptionStatus getStatus() {
        return status;
    }

    public void setStatus(ExemptionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MealExemption{" +
                "exemptionId=" + exemptionId +
                ", targetDate=" + targetDate +
                ", mealSlot=" + mealSlot +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
