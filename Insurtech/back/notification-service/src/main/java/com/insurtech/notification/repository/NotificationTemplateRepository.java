package com.insurtech.notification.repository;

import com.insurtech.notification.model.entity.NotificationTemplate;
import com.insurtech.notification.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCode(String code);

    @Query(value = "SELECT t FROM NotificationTemplate t WHERE t.code = :code")
    @Transactional(readOnly = true)
    NotificationTemplate findByCodeDirect(@Param("code") String code);

    List<NotificationTemplate> findByType(NotificationType type);

    List<NotificationTemplate> findByIsActiveTrue();

    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.type = :type")
    List<NotificationTemplate> findActiveTemplatesByType(@Param("type") NotificationType type);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.eventType = :eventType AND t.isActive = true")
    List<NotificationTemplate> findActiveTemplatesByEventType(@Param("eventType") String eventType);

    @Query("SELECT t FROM NotificationTemplate t WHERE " +
            "(:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:code IS NULL OR LOWER(t.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:active IS NULL OR t.isActive = :active) AND " +
            "(:eventType IS NULL OR t.eventType = :eventType)")
    Page<NotificationTemplate> searchTemplates(
            @Param("name") String name,
            @Param("code") String code,
            @Param("type") NotificationType type,
            @Param("active") Boolean active,
            @Param("eventType") String eventType,
            Pageable pageable);

    boolean existsByCode(String code);
}