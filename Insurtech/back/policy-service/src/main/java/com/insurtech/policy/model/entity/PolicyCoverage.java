package com.insurtech.policy.model.entity;

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
@Table(name = "POLICY_COVERAGES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PolicyCoverage {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICY_COVERAGES")
    @SequenceGenerator(name = "SEQ_POLICY_COVERAGES", sequenceName = "SEQ_POLICY_COVERAGES", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "POLICY_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @ManyToOne
    @JoinColumn(name = "COVERAGE_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Coverage coverage;

    @Column(name = "SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "PREMIUM", precision = 19, scale = 2)
    private BigDecimal premium;

    @Column(name = "PREMIUM_RATE", precision = 10, scale = 6)
    private BigDecimal premiumRate;

    @Column(name = "DEDUCTIBLE", precision = 19, scale = 2)
    private BigDecimal deductible;

    @Column(name = "DEDUCTIBLE_TYPE")
    @Enumerated(EnumType.STRING)
    private DeductibleType deductibleType;

    @Column(name = "IS_MANDATORY")
    private boolean isMandatory;

    @Column(name = "ADDITIONAL_DATA", columnDefinition = "CLOB")
    private String additionalData;  // JSON con datos específicos de la cobertura

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

    public enum DeductibleType {
        FIXED, PERCENTAGE
    }
}