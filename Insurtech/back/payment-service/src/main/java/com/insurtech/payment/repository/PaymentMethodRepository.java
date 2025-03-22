package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByPaymentMethodNumber(String paymentMethodNumber);

    List<PaymentMethod> findByCustomerNumber(String customerNumber);

    List<PaymentMethod> findByCustomerNumberAndIsActiveTrue(String customerNumber);

    Optional<PaymentMethod> findByCustomerNumberAndIsDefaultTrue(String customerNumber);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customerNumber = :customerNumber AND pm.isDefault = true")
    Optional<PaymentMethod> findDefaultByCustomerNumber(@Param("customerNumber") String customerNumber);

    List<PaymentMethod> findByMethodType(PaymentMethod.MethodType methodType);

    List<PaymentMethod> findByMethodTypeAndIsActiveTrue(PaymentMethod.MethodType methodType);

    List<PaymentMethod> findByCustomerNumberAndIsVerifiedTrue(String customerNumber);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = true AND pm.methodType IN ('CREDIT_CARD', 'DEBIT_CARD') AND pm.cardExpiryDate = :expiryMonth")
    List<PaymentMethod> findCardsByExpiryMonth(@Param("expiryMonth") YearMonth expiryMonth);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customerNumber = :customerNumber AND pm.maskedCardNumber LIKE %:lastFourDigits")
    List<PaymentMethod> findCardsByLastFourDigits(
            @Param("customerNumber") String customerNumber,
            @Param("lastFourDigits") String lastFourDigits);

    List<PaymentMethod> findByBankName(String bankName);

    List<PaymentMethod> findByWalletProvider(String walletProvider);

    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
            "LOWER(pm.paymentMethodNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PaymentMethod> searchPaymentMethods(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = true AND pm.methodType IN ('CREDIT_CARD', 'DEBIT_CARD') AND pm.cardExpiryDate < :currentMonth")
    List<PaymentMethod> findExpiredCards(@Param("currentMonth") YearMonth currentMonth);
}