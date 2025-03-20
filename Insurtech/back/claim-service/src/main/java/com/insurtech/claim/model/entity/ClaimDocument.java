package com.insurtech.claim.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM_DOCUMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClaimDocument {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIM_DOCUMENTS")
    @SequenceGenerator(name = "SEQ_CLAIM_DOCUMENTS", sequenceName = "SEQ_CLAIM_DOCUMENTS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CLAIM_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Claim claim;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DOCUMENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "FILE_PATH")
    private String filePath;

    @Column(name = "MIME_TYPE")
    private String mimeType;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "DOCUMENT_ID")
    private String documentId;  // ID en el servicio de documentos

    @Column(name = "EXTERNAL_URL")
    private String externalUrl;

    @Column(name = "VERIFIED")
    private boolean verified;

    @Column(name = "UPLOAD_DATE", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "UPLOADED_BY")
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    public enum DocumentType {
        INCIDENT_REPORT, POLICE_REPORT, MEDICAL_REPORT, INVOICE, RECEIPT, ESTIMATE,
        PHOTO, REPAIR_QUOTE, EXPERT_ASSESSMENT, ID_DOCUMENT, CONTRACT, POLICY_DOCUMENT, OTHER
    }
}