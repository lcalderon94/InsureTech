package com.insurtech.notification.model.entity;

import com.insurtech.notification.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_BATCHES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String batchNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = "total_notifications", nullable = false)
    private Integer totalNotifications;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private Set<Notification> notifications = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Version
    private Long version;

    public void addNotification(Notification notification) {
        notifications.add(notification);
        notification.setBatch(this);
    }

    public void updateCounts() {
        this.processedCount = (int) notifications.stream()
                .filter(n -> n.getStatus() != NotificationStatus.PENDING)
                .count();

        this.successCount = (int) notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.SENT)
                .count();

        this.failedCount = (int) notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.FAILED)
                .count();

        if (processedCount.equals(totalNotifications)) {
            this.status = (failedCount == 0) ? NotificationStatus.SENT :
                    (successCount == 0) ? NotificationStatus.FAILED :
                            NotificationStatus.PARTIALLY_SENT;
            this.completedAt = LocalDateTime.now();
        }
    }
}