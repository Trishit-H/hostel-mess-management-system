package com.hostel.mess;

import com.hostel.mess.config.CustomAuthenticationSuccessHandler;
import com.hostel.mess.config.SecurityConfig;
import com.hostel.mess.controller.*;
import com.hostel.mess.entity.Role;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import com.hostel.mess.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        HomeController.class,
        AuthController.class,
        AdminController.class,
        StudentController.class,
        MessStaffController.class
})
@Import({SecurityConfig.class, CustomAuthenticationSuccessHandler.class, CustomUserDetailsService.class})
class AuthenticationAndRoleAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.hostel.mess.service.StudentService studentService;

    @MockBean
    private com.hostel.mess.service.MealExemptionService mealExemptionService;

    @MockBean
    private com.hostel.mess.service.BillingService billingService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // =========================================================================
    // 1. Unauthenticated / Public Page Access Tests
    // =========================================================================

    @Test
    @DisplayName("Public: Root URL redirects directly to login for unauthenticated users")
    void rootRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Authenticated: Root URL redirects ADMIN directly to admin dashboard")
    void rootRedirectsAdminToDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Public: Login page is accessible without login")
    void publicLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Public: Unauthenticated user redirected to login when accessing admin dashboard")
    void unauthenticatedRedirectedFromAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Public: Unauthenticated user redirected to login when accessing student dashboard")
    void unauthenticatedRedirectedFromStudent() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Public: Unauthenticated user redirected to login when accessing mess dashboard")
    void unauthenticatedRedirectedFromMess() throws Exception {
        mockMvc.perform(get("/mess/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // 2. Role-Based Access Control Tests: STUDENT Role
    // =========================================================================

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("STUDENT: Can access student dashboard")
    void studentCanAccessStudentDashboard() throws Exception {
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(
                new User("student1", "pass", "Rahul Sharma", "B-204", Role.STUDENT, true)
        ));
        when(mealExemptionService.getRecentExemptions(any())).thenReturn(java.util.List.of());
        when(billingService.calculateRebateForMonth(any(), any())).thenReturn(
                new com.hostel.mess.service.dto.RebateSummary("2026-11", 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
        );

        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("STUDENT: Denied access to admin dashboard (returns 403 Forbidden)")
    void studentDeniedAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("STUDENT: Denied access to mess dashboard (returns 403 Forbidden)")
    void studentDeniedAccessToMessDashboard() throws Exception {
        mockMvc.perform(get("/mess/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    // =========================================================================
    // 3. Role-Based Access Control Tests: ADMIN Role
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN: Can access admin dashboard")
    void adminCanAccessAdminDashboard() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(
                new User("admin", "pass", "System Admin", "ADMIN-01", Role.ADMIN, true)
        ));
        when(mealExemptionService.getExpectedAttendance(any())).thenReturn(
                new com.hostel.mess.service.dto.AttendanceSummary(java.time.LocalDate.now(), 10L)
        );
        when(billingService.getAllBills()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN: Denied access to student dashboard (returns 403 Forbidden)")
    void adminDeniedAccessToStudentDashboard() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    // =========================================================================
    // 4. Role-Based Access Control Tests: MESS_STAFF Role
    // =========================================================================

    @Test
    @WithMockUser(username = "staff1", roles = {"MESS_STAFF"})
    @DisplayName("MESS_STAFF: Can access mess staff dashboard")
    void messStaffCanAccessMessDashboard() throws Exception {
        when(userRepository.findByUsername("staff1")).thenReturn(Optional.of(
                new User("staff1", "pass", "Chef Ramesh", "MESS-01", Role.MESS_STAFF, true)
        ));
        when(mealExemptionService.getExpectedAttendance(any())).thenReturn(
                new com.hostel.mess.service.dto.AttendanceSummary(java.time.LocalDate.now(), 10L)
        );

        mockMvc.perform(get("/mess/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("mess/dashboard"));
    }

    @Test
    @WithMockUser(username = "staff1", roles = {"MESS_STAFF"})
    @DisplayName("MESS_STAFF: Denied access to admin dashboard (returns 403 Forbidden)")
    void messStaffDeniedAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    // =========================================================================
    // 5. Inactive Account Login Restriction Test
    // =========================================================================

    @Test
    @DisplayName("Inactive User: Account is marked disabled in UserDetails")
    void inactiveUserIsDisabled() {
        User inactiveUser = new User("inactive1", "pass", "Inactive User", "A-101", Role.STUDENT, false);
        when(userRepository.findByUsername("inactive1")).thenReturn(Optional.of(inactiveUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("inactive1");

        assertFalse(userDetails.isEnabled(), "Inactive account must have isEnabled() == false");
    }

    @Test
    @DisplayName("Active User: Account is marked enabled in UserDetails")
    void activeUserIsEnabled() {
        User activeUser = new User("active1", "pass", "Active User", "A-102", Role.STUDENT, true);
        when(userRepository.findByUsername("active1")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("active1");

        assertTrue(userDetails.isEnabled(), "Active account must have isEnabled() == true");
    }

    // =========================================================================
    // 6. Logout Test
    // =========================================================================

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    @DisplayName("Logout: Successfully invalidates session and redirects to /login?logout=true")
    void logoutRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
