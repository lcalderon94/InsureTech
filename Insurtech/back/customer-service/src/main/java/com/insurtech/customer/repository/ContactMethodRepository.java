package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.ContactMethod;
import com.insurtech.customer.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactMethodRepository extends JpaRepository<ContactMethod, Long> {

    List<ContactMethod> findByCustomer(Customer customer);

    List<ContactMethod> findByCustomerId(Long customerId);

    Optional<ContactMethod> findByCustomerIdAndIsPrimaryTrue(Long customerId);

    List<ContactMethod> findByContactTypeAndIsVerifiedTrue(ContactMethod.ContactType contactType);

    @Query("SELECT cm FROM ContactMethod cm WHERE cm.customer.id = :customerId AND cm.contactType = :contactType")
    List<ContactMethod> findByCustomerIdAndContactType(@Param("customerId") Long customerId,
                                                       @Param("contactType") ContactMethod.ContactType contactType);

    @Query("SELECT cm FROM ContactMethod cm WHERE cm.contactType = :contactType AND cm.isOptedIn = true")
    List<ContactMethod> findOptedInContactsByType(@Param("contactType") ContactMethod.ContactType contactType);

    @Query("SELECT cm FROM ContactMethod cm WHERE " +
            "cm.contactType = :contactType AND " +
            "cm.contactValue LIKE %:value%")
    List<ContactMethod> findByContactTypeAndValueContains(
            @Param("contactType") ContactMethod.ContactType contactType,
            @Param("value") String value);
}