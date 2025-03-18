package com.insurtech.policy.model.dto;

import com.insurtech.policy.model.entity.PolicyNote;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNoteDto {

    private Long id;

    private Long policyId;

    private PolicyNote.NoteType noteType;

    private String title;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    private boolean isImportant;

    private boolean isSystemGenerated;

    private LocalDateTime createdAt;

    private String createdBy;
}