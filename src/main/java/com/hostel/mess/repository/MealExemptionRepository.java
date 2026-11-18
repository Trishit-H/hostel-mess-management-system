package com.hostel.mess.repository;

import com.hostel.mess.entity.ExemptionStatus;
import com.hostel.mess.entity.MealExemption;
import com.hostel.mess.entity.MealSlot;
import com.hostel.mess.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for MealExemption entity operations.
 */
@Repository
public interface MealExemptionRepository extends JpaRepository<MealExemption, Long> {

    List<MealExemption> findByUserOrderByTargetDateDesc(User user);

    List<MealExemption> findTop10ByUserOrderByTargetDateDesc(User user);

    List<MealExemption> findTop10ByOrderByCreatedAtDesc();

    List<MealExemption> findAllByOrderByTargetDateDescCreatedAtDesc();

    boolean existsByUserAndTargetDateAndMealSlot(User user, LocalDate targetDate, MealSlot mealSlot);

    Optional<MealExemption> findByUserAndTargetDateAndMealSlot(User user, LocalDate targetDate, MealSlot mealSlot);

    long countByTargetDateAndMealSlotAndStatus(LocalDate targetDate, MealSlot mealSlot, ExemptionStatus status);

    List<MealExemption> findByTargetDateAndMealSlotAndStatus(LocalDate targetDate, MealSlot mealSlot, ExemptionStatus status);

    List<MealExemption> findByTargetDateAndStatus(LocalDate targetDate, ExemptionStatus status);

    long countByUserAndTargetDateBetweenAndStatus(User user, LocalDate startDate, LocalDate endDate, ExemptionStatus status);

    List<MealExemption> findByUserAndTargetDateBetweenAndStatus(User user, LocalDate startDate, LocalDate endDate, ExemptionStatus status);
}
