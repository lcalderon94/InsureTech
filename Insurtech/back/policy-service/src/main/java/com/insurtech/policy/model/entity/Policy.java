package com.insurtech.policy.model.entity;

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
@Table(name = "POLICIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Policy {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICIES")
    @SequenceGenerator(name = "SEQ_POLICIES", sequenceName = "SEQ_POLICIES", allocationSize = 1)
    private Long id;

    @Column(name = "POLICY_NUMBER", unique = true, nullable = false)
    private String policyNumber;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "CUSTOMER_NUMBER")
    private String customerNumber;

    @Column(name = "POLICY_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyStatus status = PolicyStatus.DRAFT;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "ISSUE_DATE")
    private LocalDate issueDate;

    @Column(name = "PREMIUM", precision = 19, scale = 2)
    private BigDecimal premium;

    @Column(name = "SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "PAYMENT_FREQUENCY")
    @Enumerated(EnumType.STRING)
    private PaymentFrequency paymentFrequency;

    @Column(name = "PAYMENT_METHOD")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<PolicyCoverage> coverages = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<PolicyVersion> versions = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<PolicyNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<PolicyDocument> documents = new HashSet<>();

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

    public enum PolicyType {
        AUTO, HOME, LIFE, HEALTH, TRAVEL, BUSINESS, LIABILITY, OTHER
    }

    public enum PolicyStatus {
        DRAFT, QUOTED, PENDING_APPROVAL, APPROVED, ACTIVE,
        PENDING_RENEWAL, RENEWED, CANCELLED, EXPIRED, SUSPENDED
    }

    public enum PaymentFrequency {
        MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL, ONE_TIME
    }

    public enum PaymentMethod {
        BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, CASH, OTHER
    }

    // Métodos para gestionar relaciones
    public void addCoverage(PolicyCoverage coverage) {
        coverages.add(coverage);
        coverage.setPolicy(this);
    }

    public void removeCoverage(PolicyCoverage coverage) {
        coverages.remove(coverage);
        coverage.setPolicy(null);
    }

    public void addNote(PolicyNote note) {
        notes.add(note);
        note.setPolicy(this);
    }

    public void removeNote(PolicyNote note) {
        notes.remove(note);
        note.setPolicy(null);
    }

    public void addDocument(PolicyDocument document) {
        documents.add(document);
        document.setPolicy(this);
    }

    public void removeDocument(PolicyDocument document) {
        documents.remove(document);
        document.setPolicy(null);
    }

    public void addVersion(PolicyVersion version) {
        versions.add(version);
        version.setPolicy(this);
    }
}