package com.insurtech.notification.repository;

import com.insurtech.notification.model.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByNotificationId(UUID notificationId);

    List<NotificationLog> findByNotificationNumber(String notificationNumber);

    @Query("SELECT l FROM NotificationLog l WHERE " +
            "(:notificationNumber IS NULL OR LOWER(l.notificationNumber) LIKE LOWER(CONCAT('%', :notificationNumber, '%'))) AND " +
            "(:action IS NULL OR LOWER(l.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
            "(:userId IS NULL OR LOWER(l.userId) LIKE LOWER(CONCAT('%', :userId, '%'))) AND " +
            "(:fromDate IS NULL OR l.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR l.createdAt <= :toDate)")
    Page<NotificationLog> searchLogs(
            @Param("notificationNumber") String notificationNumber,
            @Param("action") String action,
            @Param("userId") String userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}