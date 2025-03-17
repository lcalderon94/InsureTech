package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.CustomerRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRelationshipRepository extends JpaRepository<CustomerRelationship, Long> {

    List<CustomerRelationship> findByPrimaryCustomerId(Long primaryCustomerId);

    List<CustomerRelationship> findByRelatedCustomerId(Long relatedCustomerId);

    List<CustomerRelationship> findByRelationshipType(CustomerRelationship.RelationshipType relationshipType);

    @Query("SELECT cr FROM CustomerRelationship cr WHERE " +
            "(cr.primaryCustomer.id = :customerId OR cr.relatedCustomer.id = :customerId) AND " +
            "cr.isActive = true")
    List<CustomerRelationship> findAllActiveRelationshipsForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT cr FROM CustomerRelationship cr WHERE " +
            "cr.primaryCustomer.id = :primaryId AND " +
            "cr.relatedCustomer.id = :relatedId")
    List<CustomerRelationship> findRelationshipBetweenCustomers(
            @Param("primaryId") Long primaryId,
            @Param("relatedId") Long relatedId);
}