package com.insurtech.notification.repository;

import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByNotificationNumber(String notificationNumber);

    List<Notification> findByStatus(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.scheduledTime <= :now")
    List<Notification> findScheduledNotificationsToProcess(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.status IN :statuses AND n.type = :type")
    List<Notification> findByStatusInAndType(
            @Param("statuses") List<NotificationStatus> statuses,
            @Param("type") String type);

    Page<Notification> findByRecipientContainingIgnoreCase(String recipient, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE " +
            "(:recipient IS NULL OR LOWER(n.recipient) LIKE LOWER(CONCAT('%', :recipient, '%'))) AND " +
            "(:subject IS NULL OR LOWER(n.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) AND " +
            "(:status IS NULL OR n.status = :status) AND " +
            "(:type IS NULL OR n.type = :type) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    Page<Notification> searchNotifications(
            @Param("recipient") String recipient,
            @Param("subject") String subject,
            @Param("status") NotificationStatus status,
            @Param("type") String type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    List<Notification> findBySourceEventId(String sourceEventId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status")
    long countByStatus(@Param("status") NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt >= :startDate AND n.createdAt <= :endDate")
    long countByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}