package com.hostel.mess.repository;

import com.hostel.mess.entity.Role;
import com.hostel.mess.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    List<User> findByRoleOrderByFullNameAsc(Role role);

    List<User> findByRoleAndActiveTrue(Role role);

    long countByRoleAndActiveTrue(Role role);

    long countByRole(Role role);
}
