package com.insurtech.notification.model.entity;

import com.insurtech.notification.model.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "DELIVERY_ATTEMPTS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(name = "status_message", length = 500)
    private String statusMessage;

    @Column(name = "provider_response", length = 1000)
    private String providerResponse;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @CreationTimestamp
    @Column(name = "attempt_time", nullable = false, updatable = false)
    private LocalDateTime attemptTime;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

}