package com.insurtech.policy.repository;

import com.insurtech.policy.model.entity.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    Optional<Coverage> findByCode(String code);

    List<Coverage> findByIsActiveTrue();

    List<Coverage> findByCoverageType(Coverage.CoverageType coverageType);

    @Query("SELECT c FROM Coverage c WHERE c.isActive = true AND " +
            "(c.policyTypes LIKE %:policyType% OR c.policyTypes IS NULL)")
    List<Coverage> findActiveCoveragesByPolicyType(@Param("policyType") String policyType);

    @Query("SELECT c FROM Coverage c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Coverage> search(@Param("searchTerm") String searchTerm);
}