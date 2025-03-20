package com.insurtech.claim.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM_ITEMS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClaimItem {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIM_ITEMS")
    @SequenceGenerator(name = "SEQ_CLAIM_ITEMS", sequenceName = "SEQ_CLAIM_ITEMS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CLAIM_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Claim claim;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "CLAIMED_AMOUNT", precision = 19, scale = 2, nullable = false)
    private BigDecimal claimedAmount;

    @Column(name = "APPROVED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "REJECTION_REASON", length = 1000)
    private String rejectionReason;

    @Column(name = "COVERED")
    private boolean covered;

    @Column(name = "EVIDENCE_DOCUMENT_ID")
    private String evidenceDocumentId;

    @Column(name = "ADDITIONAL_DETAILS", length = 2000)
    private String additionalDetails;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Version
    private Long version;
}