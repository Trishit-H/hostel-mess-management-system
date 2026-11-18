package com.hostel.mess;

import com.hostel.mess.entity.*;
import com.hostel.mess.repository.BillingLedgerRepository;
import com.hostel.mess.repository.MealExemptionRepository;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.MealExemptionService;
import com.hostel.mess.service.StudentService;
import com.hostel.mess.service.dto.AttendanceSummary;
import com.hostel.mess.service.dto.RebateSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealExemptionAndBillingBusinessTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MealExemptionRepository mealExemptionRepository;

    @Mock
    private BillingLedgerRepository billingLedgerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StudentService studentService;
    private MealExemptionService mealExemptionService;
    private BillingService billingService;

    private User sampleStudent;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(userRepository, passwordEncoder);
        mealExemptionService = new MealExemptionService(mealExemptionRepository, userRepository);
        billingService = new BillingService(billingLedgerRepository, mealExemptionService, studentService);

        ReflectionTestUtils.setField(billingService, "baseMonthlyFee", new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(billingService, "rebatePerMeal", new BigDecimal("40.00"));

        sampleStudent = new User("student1", "hashedPass", "Rahul Sharma", "B-204", Role.STUDENT, true);
        sampleStudent.setUserId(1L);
    }

    // =========================================================================
    // 1. Cutoff Calculation & Enforcement Tests
    // =========================================================================

    @Test
    @DisplayName("Cutoff: Breakfast cutoff is strictly 8:00 PM on previous calendar day")
    void testBreakfastCutoff() {
        LocalDate target = LocalDate.of(2026, 11, 20);
        LocalDateTime cutoff = mealExemptionService.getCutoffDateTime(target, MealSlot.BREAKFAST);

        assertEquals(LocalDate.of(2026, 11, 19), cutoff.toLocalDate());
        assertEquals(20, cutoff.getHour());
        assertEquals(0, cutoff.getMinute());

        // Test before cutoff: 19th Nov 7:59 PM -> Not passed
        assertFalse(mealExemptionService.isCutoffPassed(target, MealSlot.BREAKFAST,
                LocalDateTime.of(2026, 11, 19, 19, 59)));

        // Test after cutoff: 19th Nov 8:01 PM -> Passed
        assertTrue(mealExemptionService.isCutoffPassed(target, MealSlot.BREAKFAST,
                LocalDateTime.of(2026, 11, 19, 20, 1)));
    }

    @Test
    @DisplayName("Cutoff: Lunch cutoff is strictly 8:00 AM on the same day")
    void testLunchCutoff() {
        LocalDate target = LocalDate.of(2026, 11, 20);
        LocalDateTime cutoff = mealExemptionService.getCutoffDateTime(target, MealSlot.LUNCH);

        assertEquals(LocalDate.of(2026, 11, 20), cutoff.toLocalDate());
        assertEquals(8, cutoff.getHour());
        assertEquals(0, cutoff.getMinute());

        // Test before cutoff: 20th Nov 7:59 AM -> Not passed
        assertFalse(mealExemptionService.isCutoffPassed(target, MealSlot.LUNCH,
                LocalDateTime.of(2026, 11, 20, 7, 59)));

        // Test after cutoff: 20th Nov 8:01 AM -> Passed
        assertTrue(mealExemptionService.isCutoffPassed(target, MealSlot.LUNCH,
                LocalDateTime.of(2026, 11, 20, 8, 1)));
    }

    @Test
    @DisplayName("Cutoff: Dinner cutoff is strictly 8:00 AM on the same day")
    void testDinnerCutoff() {
        LocalDate target = LocalDate.of(2026, 11, 20);
        LocalDateTime cutoff = mealExemptionService.getCutoffDateTime(target, MealSlot.DINNER);

        assertEquals(LocalDate.of(2026, 11, 20), cutoff.toLocalDate());
        assertEquals(8, cutoff.getHour());
        assertEquals(0, cutoff.getMinute());

        // Test before cutoff: 20th Nov 7:59 AM -> Not passed
        assertFalse(mealExemptionService.isCutoffPassed(target, MealSlot.DINNER,
                LocalDateTime.of(2026, 11, 20, 7, 59)));

        // Test after cutoff: 20th Nov 8:01 AM -> Passed
        assertTrue(mealExemptionService.isCutoffPassed(target, MealSlot.DINNER,
                LocalDateTime.of(2026, 11, 20, 8, 1)));
    }

    // =========================================================================
    // 2. Exemption Submission & Duplicate Prevention
    // =========================================================================

    @Test
    @DisplayName("Exemption: Prevent duplicate approved exemptions for same user, date and meal")
    void testDuplicateExemptionPrevention() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        MealExemption activeExemption = new MealExemption(sampleStudent, futureDate, MealSlot.LUNCH, ExemptionStatus.APPROVED);

        when(mealExemptionRepository.findByUserAndTargetDateAndMealSlot(sampleStudent, futureDate, MealSlot.LUNCH))
                .thenReturn(Optional.of(activeExemption));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                mealExemptionService.submitExemption(sampleStudent, futureDate, MealSlot.LUNCH)
        );

        assertTrue(thrown.getMessage().contains("already have an active APPROVED exemption"));
        verify(mealExemptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exemption: Cannot cancel another student's exemption")
    void testCannotCancelOthersExemption() {
        User otherStudent = new User("otherStudent", "pass", "Other", "A-1", Role.STUDENT, true);
        otherStudent.setUserId(99L);

        MealExemption exemption = new MealExemption(otherStudent, LocalDate.now().plusDays(3), MealSlot.DINNER, ExemptionStatus.APPROVED);
        exemption.setExemptionId(10L);

        when(mealExemptionRepository.findById(10L)).thenReturn(Optional.of(exemption));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                mealExemptionService.cancelExemption(10L, sampleStudent)
        );

        assertTrue(thrown.getMessage().contains("Access Denied"));
    }

    // =========================================================================
    // 3. Expected Attendance Calculation Tests
    // =========================================================================

    @Test
    @DisplayName("Attendance: Expected Diners = Active Students - Approved Exemptions")
    void testAttendanceCalculation() {
        LocalDate date = LocalDate.of(2026, 11, 25);

        // 50 active students in mess
        when(userRepository.countByRoleAndActiveTrue(Role.STUDENT)).thenReturn(50L);

        // 5 breakfast leaves, 8 lunch leaves, 2 dinner leaves
        List<MealExemption> approvedExemptions = List.of(
                new MealExemption(sampleStudent, date, MealSlot.BREAKFAST, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.BREAKFAST, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.BREAKFAST, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.BREAKFAST, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.BREAKFAST, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.LUNCH, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.LUNCH, ExemptionStatus.APPROVED),
                new MealExemption(sampleStudent, date, MealSlot.DINNER, ExemptionStatus.APPROVED)
        );

        when(mealExemptionRepository.findByTargetDateAndStatus(date, ExemptionStatus.APPROVED))
                .thenReturn(approvedExemptions);

        AttendanceSummary summary = mealExemptionService.getExpectedAttendance(date);

        assertEquals(50L, summary.getTotalActiveStudents());
        assertEquals(5, summary.getBreakfastExemptionsCount());
        assertEquals(45L, summary.getBreakfastExpectedDiners()); // 50 - 5 = 45

        assertEquals(2, summary.getLunchExemptionsCount());
        assertEquals(48L, summary.getLunchExpectedDiners()); // 50 - 2 = 48

        assertEquals(1, summary.getDinnerExemptionsCount());
        assertEquals(49L, summary.getDinnerExpectedDiners()); // 50 - 1 = 49
    }

    // =========================================================================
    // 4. Rebate & Monthly Billing Calculation Tests
    // =========================================================================

    @Test
    @DisplayName("Rebate: Total Deduction = Number of Approved Exemptions * Rebate per meal")
    void testRebateCalculation() {
        String billingMonth = "2026-11";
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        // Student has 10 approved exempted meals in November
        when(mealExemptionRepository.countByUserAndTargetDateBetweenAndStatus(
                sampleStudent, start, end, ExemptionStatus.APPROVED
        )).thenReturn(10L);

        RebateSummary summary = billingService.calculateRebateForMonth(sampleStudent, billingMonth);

        assertEquals(10, summary.getEligibleRebateUnits());
        assertEquals(new BigDecimal("40.00"), summary.getRebatePerMeal());
        assertEquals(new BigDecimal("400.00"), summary.getTotalDeduction()); // 10 * 40 = 400
        assertEquals(new BigDecimal("3000.00"), summary.getBaseMonthlyFee());
        assertEquals(new BigDecimal("2600.00"), summary.getNetPayable());     // 3000 - 400 = 2600
    }

    @Test
    @DisplayName("Billing: Prevents duplicate bills for same student and month")
    void testDuplicateBillPrevention() {
        String billingMonth = "2026-11";

        when(billingLedgerRepository.existsByUserAndBillingMonth(sampleStudent, billingMonth))
                .thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                billingService.generateBillForStudent(sampleStudent, billingMonth)
        );

        assertTrue(thrown.getMessage().contains("already exists"));
        verify(billingLedgerRepository, never()).save(any());
    }

    // =========================================================================
    // 5. Student Management Tests
    // =========================================================================

    @Test
    @DisplayName("Student: Duplicate username is rejected on enrollment")
    void testDuplicateUsernameRejected() {
        when(userRepository.existsByUsername("student1")).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                studentService.createStudent("student1", "pass", "Duplicate Student", "C-101")
        );

        assertTrue(thrown.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Student: Toggle status activates / deactivates student")
    void testToggleStudentStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        assertTrue(sampleStudent.isActive());

        studentService.toggleStudentStatus(1L);

        assertFalse(sampleStudent.isActive());
        verify(userRepository).save(sampleStudent);
    }
}
