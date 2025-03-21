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

/**
 * Repositorio para operaciones de persistencia de métodos de pago
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * Busca un método de pago por su número único
     */
    Optional<PaymentMethod> findByPaymentMethodNumber(String paymentMethodNumber);

    /**
     * Busca todos los métodos de pago de un cliente específico
     */
    List<PaymentMethod> findByCustomerNumber(String customerNumber);

    /**
     * Busca todos los métodos de pago activos de un cliente específico
     */
    List<PaymentMethod> findByCustomerNumberAndIsActiveTrue(String customerNumber);

    /**
     * Busca el método de pago predeterminado de un cliente
     */
    Optional<PaymentMethod> findByCustomerNumberAndIsDefaultTrue(String customerNumber);

    /**
     * Busca métodos de pago por tipo
     */
    List<PaymentMethod> findByMethodType(PaymentMethod.MethodType methodType);

    /**
     * Busca métodos de pago activos por tipo
     */
    List<PaymentMethod> findByMethodTypeAndIsActiveTrue(PaymentMethod.MethodType methodType);

    /**
     * Busca métodos de pago verificados de un cliente
     */
    List<PaymentMethod> findByCustomerNumberAndIsVerifiedTrue(String customerNumber);

    /**
     * Busca tarjetas que estén por expirar en un mes específico
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = true AND pm.methodType IN ('CREDIT_CARD', 'DEBIT_CARD') AND pm.cardExpiryDate = :expiryMonth")
    List<PaymentMethod> findCardsByExpiryMonth(@Param("expiryMonth") YearMonth expiryMonth);

    /**
     * Busca tarjetas por los últimos 4 dígitos
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.customerNumber = :customerNumber AND pm.maskedCardNumber LIKE %:lastFourDigits")
    List<PaymentMethod> findCardsByLastFourDigits(
            @Param("customerNumber") String customerNumber,
            @Param("lastFourDigits") String lastFourDigits);

    /**
     * Busca métodos de pago por banco
     */
    List<PaymentMethod> findByBankName(String bankName);

    /**
     * Busca métodos de pago por proveedor de monedero electrónico
     */
    List<PaymentMethod> findByWalletProvider(String walletProvider);

    /**
     * Búsqueda por término (número de método de pago, número de cliente, nombre del método)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
            "LOWER(pm.paymentMethodNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PaymentMethod> searchPaymentMethods(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca métodos de pago que requieren actualización (tarjetas expiradas pero activas)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = true AND pm.methodType IN ('CREDIT_CARD', 'DEBIT_CARD') AND pm.cardExpiryDate < :currentMonth")
    List<PaymentMethod> findExpiredCards(@Param("currentMonth") YearMonth currentMonth);
}