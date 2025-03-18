package com.insurtech.policy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.insurtech.policy.model.entity.Policy;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByCustomerId(Long customerId);

    List<Policy> findByCustomerIdAndStatus(Long customerId, Policy.PolicyStatus status);

    Page<Policy> findByCustomerId(Long customerId, Pageable pageable);

    List<Policy> findByStatus(Policy.PolicyStatus status);

    List<Policy> findByPolicyType(Policy.PolicyType policyType);

    List<Policy> findByEndDateBefore(LocalDate date);

    List<Policy> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT p FROM Policy p WHERE " +
            "LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Policy> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Policy p WHERE p.endDate BETWEEN :startDate AND :endDate AND p.status = com.insurtech.policy.model.entity.Policy.PolicyStatus.ACTIVE")
    List<Policy> findPoliciesExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Policy p JOIN p.coverages pc WHERE pc.coverage.id = :coverageId")
    List<Policy> findPoliciesWithCoverage(@Param("coverageId") Long coverageId);

    @Query("SELECT COUNT(p) FROM Policy p WHERE p.customerId = :customerId")
    long countPoliciesByCustomer(@Param("customerId") Long customerId);
}