package com.insurtech.claim.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CLAIMS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Claim {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIMS")
    @SequenceGenerator(name = "SEQ_CLAIMS", sequenceName = "SEQ_CLAIMS", allocationSize = 1)
    private Long id;

    @Column(name = "CLAIM_NUMBER", unique = true, nullable = false)
    private String claimNumber;

    @Column(name = "POLICY_ID")
    private Long policyId;

    @Column(name = "POLICY_NUMBER")
    private String policyNumber;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "CUSTOMER_NUMBER")
    private String customerNumber;

    @Column(name = "INCIDENT_DATE", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "INCIDENT_DESCRIPTION", length = 4000, nullable = false)
    private String incidentDescription;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(name = "CLAIM_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimType claimType;

    @Column(name = "ESTIMATED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "APPROVED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "PAID_AMOUNT", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "DENIAL_REASON", length = 1000)
    private String denialReason;

    @Column(name = "HANDLER_COMMENTS", length = 2000)
    private String handlerComments;

    @Column(name = "CUSTOMER_CONTACT_INFO", length = 500)
    private String customerContactInfo;

    @Column(name = "SUBMISSION_DATE")
    private LocalDateTime submissionDate;

    @Column(name = "ASSESSMENT_DATE")
    private LocalDateTime assessmentDate;

    @Column(name = "APPROVAL_DATE")
    private LocalDateTime approvalDate;

    @Column(name = "SETTLEMENT_DATE")
    private LocalDateTime settlementDate;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<ClaimItem> items = new HashSet<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<ClaimDocument> documents = new HashSet<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<ClaimNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<ClaimStatusHistory> statusHistory = new HashSet<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<ClaimPayment> payments = new HashSet<>();

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

    public enum ClaimStatus {
        SUBMITTED, UNDER_REVIEW, INFORMATION_REQUESTED, ASSESSED, APPROVED,
        PARTIALLY_APPROVED, DENIED, PAYMENT_IN_PROCESS, PARTIALLY_PAID,
        PAID, CLOSED, REOPENED, WITHDRAWN, CANCELLED
    }

    public enum ClaimType {
        AUTO_ACCIDENT, AUTO_THEFT, HOME_DAMAGE, HOME_THEFT, PROPERTY_DAMAGE,
        PERSONAL_INJURY, MEDICAL, LIFE, LIABILITY, TRAVEL, BUSINESS_INTERRUPTION, OTHER
    }

    // Métodos para gestionar relaciones bidireccionales
    public void addItem(ClaimItem item) {
        items.add(item);
        item.setClaim(this);
    }

    public void removeItem(ClaimItem item) {
        items.remove(item);
        item.setClaim(null);
    }

    public void addDocument(ClaimDocument document) {
        documents.add(document);
        document.setClaim(this);
    }

    public void removeDocument(ClaimDocument document) {
        documents.remove(document);
        document.setClaim(null);
    }

    public void addNote(ClaimNote note) {
        notes.add(note);
        note.setClaim(this);
    }

    public void removeNote(ClaimNote note) {
        notes.remove(note);
        note.setClaim(null);
    }

    public void addStatusHistory(ClaimStatusHistory statusHistory) {
        this.statusHistory.add(statusHistory);
        statusHistory.setClaim(this);
    }

    public void addPayment(ClaimPayment payment) {
        payments.add(payment);
        payment.setClaim(this);
    }

    public void removePayment(ClaimPayment payment) {
        payments.remove(payment);
        payment.setClaim(null);
    }
}