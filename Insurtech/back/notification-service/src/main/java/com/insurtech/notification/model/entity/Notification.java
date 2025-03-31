package com.insurtech.notification.model.entity;

import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationStatus;
import com.insurtech.notification.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String notificationNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "cc_recipients")
    private String ccRecipients;

    @Column(name = "source_event_id")
    private String sourceEventId;

    @Column(name = "source_event_type")
    private String sourceEventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryAttempt> deliveryAttempts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private NotificationBatch batch;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "data_context")
    @Lob
    private String dataContext;

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

    public void addDeliveryAttempt(DeliveryAttempt attempt) {
        deliveryAttempts.add(attempt);
        attempt.setNotification(this);
    }
}