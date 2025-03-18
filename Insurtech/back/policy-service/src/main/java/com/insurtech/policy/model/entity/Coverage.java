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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "COVERAGES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Coverage {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_COVERAGES")
    @SequenceGenerator(name = "SEQ_COVERAGES", sequenceName = "SEQ_COVERAGES", allocationSize = 1)
    private Long id;

    @Column(name = "CODE", unique = true, nullable = false)
    private String code;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    @Column(name = "COVERAGE_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private CoverageType coverageType;

    @Column(name = "DEFAULT_PREMIUM_RATE", precision = 10, scale = 6)
    private BigDecimal defaultPremiumRate;

    @Column(name = "DEFAULT_SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal defaultSumInsured;

    @Column(name = "MINIMUM_SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal minimumSumInsured;

    @Column(name = "MAXIMUM_SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal maximumSumInsured;

    @Column(name = "IS_ACTIVE")
    private boolean isActive = true;

    @Column(name = "POLICY_TYPES")
    private String policyTypes;  // Almacena tipos de póliza compatibles separados por comas

    @OneToMany(mappedBy = "coverage")
    @EqualsAndHashCode.Exclude
    private Set<PolicyCoverage> policyCoverages = new HashSet<>();

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

    public enum CoverageType {
        PROPERTY, LIABILITY, CASUALTY, LIFE, HEALTH, INCOME_PROTECTION, OTHER
    }
}