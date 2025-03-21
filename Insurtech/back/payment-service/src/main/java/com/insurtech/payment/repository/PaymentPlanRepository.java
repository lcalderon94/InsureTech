package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de persistencia de planes de pago
 */
@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {

    /**
     * Busca un plan de pago por su número único
     */
    Optional<PaymentPlan> findByPaymentPlanNumber(String paymentPlanNumber);

    /**
     * Busca todos los planes de pago de un cliente específico
     */
    List<PaymentPlan> findByCustomerNumber(String customerNumber);

    /**
     * Busca todos los planes de pago asociados a una póliza específica
     */
    List<PaymentPlan> findByPolicyNumber(String policyNumber);

    /**
     * Busca planes de pago por estado
     */
    List<PaymentPlan> findByStatus(PaymentPlan.PlanStatus status);

    /**
     * Busca planes de pago por tipo
     */
    List<PaymentPlan> findByPlanType(PaymentPlan.PlanType planType);

    /**
     * Busca planes de pago por frecuencia
     */
    List<PaymentPlan> findByFrequency(PaymentPlan.Frequency frequency);

    /**
     * Busca planes de pago por método de pago asociado
     */
    List<PaymentPlan> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Busca planes de pago automáticos
     */
    List<PaymentPlan> findByIsAutoPaymentTrue();

    /**
     * Busca planes de pago con próxima fecha de pago dentro de un rango
     */
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.isAutoPayment = true " +
            "AND EXISTS (SELECT 1 FROM Payment p WHERE p.paymentPlan = pp) " +
            "AND (SELECT MAX(p.dueDate) FROM Payment p WHERE p.paymentPlan = pp) < :referenceDate " +
            "AND pp.lastPaymentDate IS NULL OR pp.lastPaymentDate > :currentDate")
    List<PaymentPlan> findActivePlansWithUpcomingPayments(
            @Param("referenceDate") LocalDateTime referenceDate,
            @Param("currentDate") LocalDateTime currentDate);

    /**
     * Busca planes de pago que vencen pronto
     */
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.lastPaymentDate BETWEEN :startDate AND :endDate")
    List<PaymentPlan> findPlansEndingSoon(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Búsqueda por término (número de plan, número de cliente, número de póliza)
     */
    @Query("SELECT pp FROM PaymentPlan pp WHERE " +
            "LOWER(pp.paymentPlanNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pp.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pp.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PaymentPlan> searchPaymentPlans(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca planes de pago asociados a un día específico del mes (para pagos recurrentes)
     */
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.isAutoPayment = true AND pp.paymentDay = :dayOfMonth")
    List<PaymentPlan> findActiveAutoPaymentPlansByPaymentDay(@Param("dayOfMonth") Integer dayOfMonth);

    /**
     * Busca planes de pago que están atrasados en pagos
     */
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND EXISTS " +
            "(SELECT 1 FROM Payment p WHERE p.paymentPlan = pp AND p.status = 'PENDING' AND p.dueDate < :referenceDate)")
    List<PaymentPlan> findPlansWithLatePayments(@Param("referenceDate") LocalDateTime referenceDate);
}