package com.insurtech.claim.model.dto;

import com.insurtech.claim.model.entity.ClaimDocument;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocumentDto {

    private Long id;

    private Long claimId;

    @NotBlank(message = "El título del documento es obligatorio")
    private String title;

    private String description;

    private ClaimDocument.DocumentType documentType;

    private String fileName;

    private String filePath;

    private String mimeType;

    private Long fileSize;

    private String documentId;

    private String externalUrl;

    private boolean verified;

    private LocalDateTime uploadDate;

    private String uploadedBy;
}