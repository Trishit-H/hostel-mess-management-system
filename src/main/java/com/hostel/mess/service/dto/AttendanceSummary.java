package com.hostel.mess.service.dto;

import com.hostel.mess.entity.MealExemption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing expected dining attendance for a specific date.
 * 
 * Formula: Expected Diners = Active Students - Valid (Approved) Exemptions
 */
public class AttendanceSummary {

    private LocalDate date;
    private long totalActiveStudents;

    private long breakfastExemptionsCount;
    private long breakfastExpectedDiners;

    private long lunchExemptionsCount;
    private long lunchExpectedDiners;

    private long dinnerExemptionsCount;
    private long dinnerExpectedDiners;

    private List<MealExemption> breakfastExemptions = new ArrayList<>();
    private List<MealExemption> lunchExemptions = new ArrayList<>();
    private List<MealExemption> dinnerExemptions = new ArrayList<>();

    public AttendanceSummary() {
    }

    public AttendanceSummary(LocalDate date, long totalActiveStudents) {
        this.date = date;
        this.totalActiveStudents = totalActiveStudents;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalActiveStudents() {
        return totalActiveStudents;
    }

    public void setTotalActiveStudents(long totalActiveStudents) {
        this.totalActiveStudents = totalActiveStudents;
    }

    public long getBreakfastExemptionsCount() {
        return breakfastExemptionsCount;
    }

    public void setBreakfastExemptionsCount(long breakfastExemptionsCount) {
        this.breakfastExemptionsCount = breakfastExemptionsCount;
    }

    public long getBreakfastExpectedDiners() {
        return breakfastExpectedDiners;
    }

    public void setBreakfastExpectedDiners(long breakfastExpectedDiners) {
        this.breakfastExpectedDiners = breakfastExpectedDiners;
    }

    public long getLunchExemptionsCount() {
        return lunchExemptionsCount;
    }

    public void setLunchExemptionsCount(long lunchExemptionsCount) {
        this.lunchExemptionsCount = lunchExemptionsCount;
    }

    public long getLunchExpectedDiners() {
        return lunchExpectedDiners;
    }

    public void setLunchExpectedDiners(long lunchExpectedDiners) {
        this.lunchExpectedDiners = lunchExpectedDiners;
    }

    public long getDinnerExemptionsCount() {
        return dinnerExemptionsCount;
    }

    public void setDinnerExemptionsCount(long dinnerExemptionsCount) {
        this.dinnerExemptionsCount = dinnerExemptionsCount;
    }

    public long getDinnerExpectedDiners() {
        return dinnerExpectedDiners;
    }

    public void setDinnerExpectedDiners(long dinnerExpectedDiners) {
        this.dinnerExpectedDiners = dinnerExpectedDiners;
    }

    public List<MealExemption> getBreakfastExemptions() {
        return breakfastExemptions;
    }

    public void setBreakfastExemptions(List<MealExemption> breakfastExemptions) {
        this.breakfastExemptions = breakfastExemptions;
    }

    public List<MealExemption> getLunchExemptions() {
        return lunchExemptions;
    }

    public void setLunchExemptions(List<MealExemption> lunchExemptions) {
        this.lunchExemptions = lunchExemptions;
    }

    public List<MealExemption> getDinnerExemptions() {
        return dinnerExemptions;
    }

    public void setDinnerExemptions(List<MealExemption> dinnerExemptions) {
        this.dinnerExemptions = dinnerExemptions;
    }
}
