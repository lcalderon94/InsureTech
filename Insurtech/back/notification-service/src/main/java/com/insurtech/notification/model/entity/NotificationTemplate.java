package com.insurtech.notification.model.entity;

import com.insurtech.notification.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;  // Añadir esta importación
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_TEMPLATES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate implements Serializable {  // Implementar la interfaz

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "required_variables", length = 1000)
    private String requiredVariables;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "event_type")
    private String eventType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Version
    private Long version;
}