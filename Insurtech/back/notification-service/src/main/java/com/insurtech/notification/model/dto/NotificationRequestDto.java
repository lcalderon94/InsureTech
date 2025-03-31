package com.insurtech.notification.model.dto;

import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification priority is required")
    private NotificationPriority priority;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    private List<@Email(message = "Invalid CC email format") String> ccRecipients;

    private String templateCode;

    private String sourceEventId;

    private String sourceEventType;

    private Map<String, Object> templateVariables;

    private LocalDateTime scheduledTime;
}