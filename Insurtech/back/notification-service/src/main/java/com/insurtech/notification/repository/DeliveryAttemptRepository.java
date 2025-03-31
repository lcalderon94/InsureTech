package com.insurtech.notification.repository;

import com.insurtech.notification.model.entity.DeliveryAttempt;
import com.insurtech.notification.model.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByNotificationId(UUID notificationId);

    List<DeliveryAttempt> findByNotificationIdOrderByAttemptNumberDesc(UUID notificationId);

    @Query("SELECT MAX(da.attemptNumber) FROM DeliveryAttempt da WHERE da.notification.id = :notificationId")
    Integer findMaxAttemptNumberByNotificationId(@Param("notificationId") UUID notificationId);

    @Query("SELECT da FROM DeliveryAttempt da WHERE da.status = :status AND da.nextRetryTime <= :now")
    List<DeliveryAttempt> findAttemptsToRetry(
            @Param("status") DeliveryStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(da) FROM DeliveryAttempt da WHERE da.status = :status AND da.attemptTime >= :startDate AND da.attemptTime <= :endDate")
    long countByStatusAndDateRange(
            @Param("status") DeliveryStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}