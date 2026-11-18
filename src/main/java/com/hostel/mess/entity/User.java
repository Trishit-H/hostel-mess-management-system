package com.hostel.mess.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a hostel resident, mess staff, or administrator.
 * 
 * Note: Mapped to the database table name "users" because "user" is a reserved
 * SQL keyword in PostgreSQL.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "room_no", length = 20)
    private String roomNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // One User can have multiple MealExemption records
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealExemption> mealExemptions = new ArrayList<>();

    // One User can have multiple BillingLedger records
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingLedger> billingLedgers = new ArrayList<>();

    // Default constructor (required by JPA)
    public User() {
    }

    // Parameterized constructor
    public User(String username, String password, String fullName, String roomNo, Role role, boolean active) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.roomNo = roomNo;
        this.role = role;
        this.active = active;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<MealExemption> getMealExemptions() {
        return mealExemptions;
    }

    public void setMealExemptions(List<MealExemption> mealExemptions) {
        this.mealExemptions = mealExemptions;
    }

    public List<BillingLedger> getBillingLedgers() {
        return billingLedgers;
    }

    public void setBillingLedgers(List<BillingLedger> billingLedgers) {
        this.billingLedgers = billingLedgers;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roomNo='" + roomNo + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
