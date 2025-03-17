package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByIdentificationNumberAndIdentificationType(String identificationNumber, String identificationType);

    boolean existsByEmail(String email);

    boolean existsByIdentificationNumberAndIdentificationType(String identificationNumber, String identificationType);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.identificationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Customer> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Customer c JOIN c.segments s WHERE s.id = :segmentId")
    List<Customer> findBySegmentId(@Param("segmentId") Long segmentId);

    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND c.dateOfBirth < :date")
    List<Customer> findActiveCustomersBornBefore(@Param("date") LocalDateTime date);

    @Query("SELECT c FROM Customer c WHERE c.updatedAt > :lastSyncDate")
    List<Customer> findUpdatedSince(@Param("lastSyncDate") LocalDateTime lastSyncDate);

    @Query(value = "SELECT * FROM CUSTOMERS c WHERE " +
            "c.RISK_PROFILE = :riskProfile AND " +
            "EXISTS (SELECT 1 FROM ADDRESSES a WHERE a.CUSTOMER_ID = c.ID AND a.CITY = :city)",
            nativeQuery = true)
    List<Customer> findByRiskProfileAndCity(@Param("riskProfile") String riskProfile, @Param("city") String city);
}