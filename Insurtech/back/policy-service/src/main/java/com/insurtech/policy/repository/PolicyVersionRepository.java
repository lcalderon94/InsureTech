package com.insurtech.policy.repository;

import com.insurtech.policy.model.entity.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {

    List<PolicyVersion> findByPolicyId(Long policyId);

    Optional<PolicyVersion> findByPolicyIdAndVersionNumber(Long policyId, Integer versionNumber);

    @Query("SELECT MAX(pv.versionNumber) FROM PolicyVersion pv WHERE pv.policy.id = :policyId")
    Integer findMaxVersionNumberByPolicyId(@Param("policyId") Long policyId);

    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId ORDER BY pv.versionNumber DESC")
    List<PolicyVersion> findByPolicyIdOrderByVersionNumberDesc(@Param("policyId") Long policyId);
}