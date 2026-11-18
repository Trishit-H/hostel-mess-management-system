package com.hostel.mess.repository;

import com.hostel.mess.entity.BillingLedger;
import com.hostel.mess.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for BillingLedger entity operations.
 */
@Repository
public interface BillingLedgerRepository extends JpaRepository<BillingLedger, Long> {

    List<BillingLedger> findByUserOrderByBillingMonthDesc(User user);

    Optional<BillingLedger> findTop1ByUserOrderByBillingMonthDesc(User user);

    Optional<BillingLedger> findByUserAndBillingMonth(User user, String billingMonth);

    boolean existsByUserAndBillingMonth(User user, String billingMonth);

    List<BillingLedger> findAllByOrderByBillingMonthDescGeneratedAtDesc();

    List<BillingLedger> findByBillingMonthOrderByUserFullNameAsc(String billingMonth);

    long countByBillingMonth(String billingMonth);
}
