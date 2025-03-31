package com.insurtech.notification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttemptDto {
    private UUID id;
    private Integer attemptNumber;
    private String status;
    private String statusMessage;
    private LocalDateTime attemptTime;
}