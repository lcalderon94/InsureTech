package com.insurtech.policy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "POLICY_DOCUMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PolicyDocument {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICY_DOCUMENTS")
    @SequenceGenerator(name = "SEQ_POLICY_DOCUMENTS", sequenceName = "SEQ_POLICY_DOCUMENTS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "POLICY_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @Column(name = "DOCUMENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;

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

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    public enum DocumentType {
        POLICY_DOCUMENT, ENDORSEMENT, INVOICE, RECEIPT, CLAIM_DOCUMENT,
        APPLICATION_FORM, TERMS_AND_CONDITIONS, OTHER
    }
}