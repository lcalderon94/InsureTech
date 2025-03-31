package com.insurtech.notification.model.dto;

import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationStatus;
import com.insurtech.notification.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private UUID id;
    private String notificationNumber;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationStatus status;
    private String subject;
    private String content;
    private String recipient;
    private List<String> ccRecipients;
    private String templateCode;
    private String sourceEventId;
    private String sourceEventType;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DeliveryAttemptDto> deliveryAttempts;
}