package com.insurtech.notification.repository;

import com.insurtech.notification.model.entity.NotificationBatch;
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
public interface NotificationBatchRepository extends JpaRepository<NotificationBatch, UUID> {

    Optional<NotificationBatch> findByBatchNumber(String batchNumber);

    List<NotificationBatch> findByStatus(NotificationStatus status);

    @Query("SELECT b FROM NotificationBatch b WHERE " +
            "(:batchNumber IS NULL OR LOWER(b.batchNumber) LIKE LOWER(CONCAT('%', :batchNumber, '%'))) AND " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:sourceReference IS NULL OR LOWER(b.sourceReference) LIKE LOWER(CONCAT('%', :sourceReference, '%'))) AND " +
            "(:sourceType IS NULL OR LOWER(b.sourceType) LIKE LOWER(CONCAT('%', :sourceType, '%'))) AND " +
            "(:fromDate IS NULL OR b.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR b.createdAt <= :toDate)")
    Page<NotificationBatch> searchBatches(
            @Param("batchNumber") String batchNumber,
            @Param("status") NotificationStatus status,
            @Param("sourceReference") String sourceReference,
            @Param("sourceType") String sourceType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT COUNT(b) FROM NotificationBatch b WHERE b.status = :status")
    long countByStatus(@Param("status") NotificationStatus status);

    @Query("SELECT COUNT(b) FROM NotificationBatch b WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate")
    long countByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}