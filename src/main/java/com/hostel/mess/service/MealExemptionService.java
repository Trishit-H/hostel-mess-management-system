package com.hostel.mess.service;

import com.hostel.mess.entity.ExemptionStatus;
import com.hostel.mess.entity.MealExemption;
import com.hostel.mess.entity.MealSlot;
import com.hostel.mess.entity.Role;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.MealExemptionRepository;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.dto.AttendanceSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Business service handling meal exemption applications, strict server-side cutoff validations,
 * cancellation policies, and expected attendance calculations.
 */
@Service
@Transactional
public class MealExemptionService {

    private final MealExemptionRepository mealExemptionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter CUTOFF_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    public MealExemptionService(MealExemptionRepository mealExemptionRepository,
                                UserRepository userRepository) {
        this.mealExemptionRepository = mealExemptionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Determines the exact cutoff timestamp for a meal slot:
     * - BREAKFAST: 8:00 PM (20:00) on the previous calendar day.
     * - LUNCH:     8:00 AM (08:00) on the same day.
     * - DINNER:    8:00 AM (08:00) on the same day.
     */
    public LocalDateTime getCutoffDateTime(LocalDate targetDate, MealSlot mealSlot) {
        if (targetDate == null || mealSlot == null) {
            throw new IllegalArgumentException("Target date and meal slot must not be null.");
        }
        if (mealSlot == MealSlot.BREAKFAST) {
            return targetDate.minusDays(1).atTime(20, 0); // 8:00 PM on previous day
        } else if (mealSlot == MealSlot.LUNCH) {
            return targetDate.atTime(8, 0);               // 8:00 AM on same day
        } else if (mealSlot == MealSlot.DINNER) {
            return targetDate.atTime(8, 0);               // 8:00 AM on same day
        }
        throw new IllegalArgumentException("Unknown meal slot: " + mealSlot);
    }

    public boolean isCutoffPassed(LocalDate targetDate, MealSlot mealSlot) {
        return isCutoffPassed(targetDate, mealSlot, LocalDateTime.now());
    }

    public boolean isCutoffPassed(LocalDate targetDate, MealSlot mealSlot, LocalDateTime currentDateTime) {
        LocalDateTime cutoff = getCutoffDateTime(targetDate, mealSlot);
        return currentDateTime.isAfter(cutoff);
    }

    /**
     * Submits and automatically approves an advance meal exemption.
     * Validates server-side that the cutoff deadline has not passed
     * and prevents duplicate submissions for the same user, date, and meal slot.
     */
    public MealExemption submitExemption(User user, LocalDate targetDate, MealSlot mealSlot) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        if (targetDate == null) {
            throw new IllegalArgumentException("Exemption date is required.");
        }
        if (mealSlot == null) {
            throw new IllegalArgumentException("Meal slot is required.");
        }

        // Validate Cutoff
        if (isCutoffPassed(targetDate, mealSlot)) {
            LocalDateTime cutoff = getCutoffDateTime(targetDate, mealSlot);
            throw new IllegalArgumentException(
                    "Cutoff deadline has passed for " + mealSlot + " on " + targetDate +
                    ". Submissions closed on " + cutoff.format(CUTOFF_FORMATTER) + "."
            );
        }

        // Check for existing record
        Optional<MealExemption> existing = mealExemptionRepository.findByUserAndTargetDateAndMealSlot(user, targetDate, mealSlot);
        if (existing.isPresent()) {
            MealExemption record = existing.get();
            if (record.getStatus() == ExemptionStatus.APPROVED) {
                throw new IllegalArgumentException(
                        "You already have an active APPROVED exemption for " + mealSlot + " on " + targetDate + "."
                );
            } else {
                // If previously cancelled, reactivate to APPROVED
                record.setStatus(ExemptionStatus.APPROVED);
                record.setCreatedAt(LocalDateTime.now());
                return mealExemptionRepository.save(record);
            }
        }

        MealExemption exemption = new MealExemption(user, targetDate, mealSlot, ExemptionStatus.APPROVED);
        return mealExemptionRepository.save(exemption);
    }

    /**
     * Cancels an existing exemption before its applicable cutoff time.
     */
    public MealExemption cancelExemption(Long exemptionId, User user) {
        MealExemption exemption = mealExemptionRepository.findById(exemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Exemption not found with ID: " + exemptionId));

        // Ensure user can only cancel their own exemption
        if (!exemption.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Access Denied: You cannot modify another resident's exemption.");
        }

        if (exemption.getStatus() == ExemptionStatus.CANCELLED) {
            return exemption;
        }

        // Validate Cutoff for cancellation
        if (isCutoffPassed(exemption.getTargetDate(), exemption.getMealSlot())) {
            LocalDateTime cutoff = getCutoffDateTime(exemption.getTargetDate(), exemption.getMealSlot());
            throw new IllegalArgumentException(
                    "Cannot cancel this exemption because the cutoff time (" +
                    cutoff.format(CUTOFF_FORMATTER) + ") has already passed."
            );
        }

        exemption.setStatus(ExemptionStatus.CANCELLED);
        return mealExemptionRepository.save(exemption);
    }

    @Transactional(readOnly = true)
    public List<MealExemption> getExemptionHistory(User user) {
        return mealExemptionRepository.findByUserOrderByTargetDateDesc(user);
    }

    @Transactional(readOnly = true)
    public List<MealExemption> getRecentExemptions(User user) {
        return mealExemptionRepository.findTop10ByUserOrderByTargetDateDesc(user);
    }

    @Transactional(readOnly = true)
    public List<MealExemption> getAllRecentExemptions() {
        return mealExemptionRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<MealExemption> getAllExemptions() {
        return mealExemptionRepository.findAllByOrderByTargetDateDescCreatedAtDesc();
    }

    /**
     * Calculates expected meal attendance for a given calendar date:
     * Expected Diners = Active Students - Approved Exemptions
     */
    @Transactional(readOnly = true)
    public AttendanceSummary getExpectedAttendance(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        long activeStudents = userRepository.countByRoleAndActiveTrue(Role.STUDENT);

        AttendanceSummary summary = new AttendanceSummary(targetDate, activeStudents);

        List<MealExemption> approved = mealExemptionRepository.findByTargetDateAndStatus(targetDate, ExemptionStatus.APPROVED);

        List<MealExemption> breakfastList = approved.stream()
                .filter(e -> e.getMealSlot() == MealSlot.BREAKFAST)
                .toList();
        List<MealExemption> lunchList = approved.stream()
                .filter(e -> e.getMealSlot() == MealSlot.LUNCH)
                .toList();
        List<MealExemption> dinnerList = approved.stream()
                .filter(e -> e.getMealSlot() == MealSlot.DINNER)
                .toList();

        summary.setBreakfastExemptions(breakfastList);
        summary.setBreakfastExemptionsCount(breakfastList.size());
        summary.setBreakfastExpectedDiners(Math.max(0, activeStudents - breakfastList.size()));

        summary.setLunchExemptions(lunchList);
        summary.setLunchExemptionsCount(lunchList.size());
        summary.setLunchExpectedDiners(Math.max(0, activeStudents - lunchList.size()));

        summary.setDinnerExemptions(dinnerList);
        summary.setDinnerExemptionsCount(dinnerList.size());
        summary.setDinnerExpectedDiners(Math.max(0, activeStudents - dinnerList.size()));

        return summary;
    }

    /**
     * Counts approved exemptions for a student in a specific billing month (YYYY-MM).
     */
    @Transactional(readOnly = true)
    public long countApprovedExemptionsInMonth(User user, LocalDate startDate, LocalDate endDate) {
        return mealExemptionRepository.countByUserAndTargetDateBetweenAndStatus(
                user, startDate, endDate, ExemptionStatus.APPROVED
        );
    }
}
