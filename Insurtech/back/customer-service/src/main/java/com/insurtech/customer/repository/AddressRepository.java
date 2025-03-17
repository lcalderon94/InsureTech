package com.insurtech.customer.repository;

import com.insurtech.customer.model.entity.Address;
import com.insurtech.customer.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomer(Customer customer);

    List<Address> findByCustomerId(Long customerId);

    Optional<Address> findByCustomerIdAndIsPrimaryTrue(Long customerId);

    List<Address> findByCity(String city);

    List<Address> findByStateAndCountry(String state, String country);

    @Query("SELECT a FROM Address a WHERE a.customer.id = :customerId AND a.addressType = :addressType")
    List<Address> findByCustomerIdAndAddressType(@Param("customerId") Long customerId,
                                                 @Param("addressType") Address.AddressType addressType);

    @Query("SELECT DISTINCT a.city FROM Address a WHERE a.country = :country")
    List<String> findDistinctCitiesByCountry(@Param("country") String country);
}