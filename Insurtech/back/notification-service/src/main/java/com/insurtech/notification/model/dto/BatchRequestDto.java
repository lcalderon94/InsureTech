package com.insurtech.notification.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequestDto {

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private String sourceReference;

    private String sourceType;

    @NotEmpty(message = "Notifications list cannot be empty")
    @Size(min = 1, max = 1000, message = "Batch can contain between 1 and 1000 notifications")
    private List<@Valid NotificationRequestDto> notifications;
}