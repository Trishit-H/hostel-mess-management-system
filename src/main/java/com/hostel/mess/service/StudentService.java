package com.hostel.mess.service;

import com.hostel.mess.entity.Role;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing student resident records, credentials, and activation state.
 */
@Service
@Transactional
public class StudentService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllStudents() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveStudents() {
        return userRepository.findByRoleAndActiveTrue(Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public User getStudentById(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
    }

    public User createStudent(String username, String password, String fullName, String roomNo) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        String trimmedUsername = username.trim().toLowerCase();
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Username '" + trimmedUsername + "' is already registered. Please choose another.");
        }

        User student = new User(
                trimmedUsername,
                passwordEncoder.encode(password.trim()),
                fullName.trim(),
                roomNo != null ? roomNo.trim() : "",
                Role.STUDENT,
                true
        );

        return userRepository.save(student);
    }

    public User updateStudent(Long id, String fullName, String roomNo, String password) {
        User student = getStudentById(id);

        if (fullName != null && !fullName.trim().isEmpty()) {
            student.setFullName(fullName.trim());
        }
        if (roomNo != null) {
            student.setRoomNo(roomNo.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            student.setPassword(passwordEncoder.encode(password.trim()));
        }

        return userRepository.save(student);
    }

    public void toggleStudentStatus(Long id) {
        User student = getStudentById(id);
        student.setActive(!student.isActive());
        userRepository.save(student);
    }

    @Transactional(readOnly = true)
    public long countActiveStudents() {
        return userRepository.countByRoleAndActiveTrue(Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public long countTotalStudents() {
        return userRepository.countByRole(Role.STUDENT);
    }
}
