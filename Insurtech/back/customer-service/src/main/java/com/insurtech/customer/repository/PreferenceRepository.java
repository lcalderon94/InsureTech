package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.Preference;
import com.insurtech.customer.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    List<Preference> findByCustomer(Customer customer);

    List<Preference> findByCustomerId(Long customerId);

    List<Preference> findByPreferenceType(Preference.PreferenceType preferenceType);

    Optional<Preference> findByCustomerIdAndPreferenceTypeAndPreferenceKey(
            Long customerId,
            Preference.PreferenceType preferenceType,
            String preferenceKey);

    @Query("SELECT p FROM Preference p WHERE " +
            "p.preferenceType = :preferenceType AND " +
            "p.preferenceKey = :preferenceKey AND " +
            "p.preferenceValue = :preferenceValue")
    List<Preference> findByTypeAndKeyAndValue(
            @Param("preferenceType") Preference.PreferenceType preferenceType,
            @Param("preferenceKey") String preferenceKey,
            @Param("preferenceValue") String preferenceValue);
}