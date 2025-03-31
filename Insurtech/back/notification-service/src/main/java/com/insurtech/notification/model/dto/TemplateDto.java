package com.insurtech.notification.model.dto;

import com.insurtech.notification.model.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TemplateDto {

    private UUID id;

    @NotBlank(message = "Template code is required")
    private String code;

    @NotBlank(message = "Template name is required")
    private String name;

    @NotNull(message = "Template type is required")
    private NotificationType type;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private Boolean isActive;

    private List<String> requiredVariables;

    private String description;

    private String eventType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}