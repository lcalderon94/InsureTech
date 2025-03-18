package com.insurtech.policy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "POLICY_VERSIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PolicyVersion {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICY_VERSIONS")
    @SequenceGenerator(name = "SEQ_POLICY_VERSIONS", sequenceName = "SEQ_POLICY_VERSIONS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "POLICY_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @Column(name = "VERSION_NUMBER", nullable = false)
    private Integer versionNumber;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private Policy.PolicyStatus status;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "PREMIUM", precision = 19, scale = 2)
    private BigDecimal premium;

    @Column(name = "SUM_INSURED", precision = 19, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "CHANGE_REASON", length = 1000)
    private String changeReason;

    @Column(name = "POLICY_DATA", columnDefinition = "CLOB")
    private String policyData;  // JSON snapshot de la póliza en este momento

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;
}