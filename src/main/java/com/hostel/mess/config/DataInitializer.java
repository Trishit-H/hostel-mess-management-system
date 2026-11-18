package com.hostel.mess.config;

import com.hostel.mess.entity.Role;
import com.hostel.mess.entity.User;
import com.hostel.mess.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Automatically seeds the initial administrator account and testing accounts
 * if no administrative user is found in the database upon startup.
 * 
 * Passwords are encrypted using BCrypt before persisting.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin.username:admin}")
    private String adminUsername;

    @Value("${app.init.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.init.admin.full-name:System Administrator}")
    private String adminFullName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed initial Administrator account if none exists
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    adminFullName,
                    "ADMIN-OFFICE",
                    Role.ADMIN,
                    true
            );
            userRepository.save(admin);
            log.info("Initialized default Administrator account: [username: {}]", adminUsername);
        }

        // 2. Seed a sample Student account for testing and viva demonstration
        if (!userRepository.existsByUsername("student1")) {
            User student = new User(
                    "student1",
                    passwordEncoder.encode("Student@123"),
                    "Rahul Sharma",
                    "B-204",
                    Role.STUDENT,
                    true
            );
            userRepository.save(student);
            log.info("Initialized sample Student account: [username: student1]");
        }

        // 3. Seed a sample Mess Staff account for testing
        if (!userRepository.existsByUsername("staff1")) {
            User staff = new User(
                    "staff1",
                    passwordEncoder.encode("Staff@123"),
                    "Ramesh Kumar (Mess Manager)",
                    "MESS-OFFICE",
                    Role.MESS_STAFF,
                    true
            );
            userRepository.save(staff);
            log.info("Initialized sample Mess Staff account: [username: staff1]");
        }

        // 4. Seed an Inactive account to test deactivated account login restriction
        if (!userRepository.existsByUsername("inactive_student")) {
            User inactive = new User(
                    "inactive_student",
                    passwordEncoder.encode("Student@123"),
                    "Amit Verma (Deactivated)",
                    "A-102",
                    Role.STUDENT,
                    false // active = false
            );
            userRepository.save(inactive);
            log.info("Initialized sample Inactive account: [username: inactive_student]");
        }
    }
}
