package com.insurtech.claim.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM_STATUS_HISTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClaimStatusHistory {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIM_STATUS_HISTORY")
    @SequenceGenerator(name = "SEQ_CLAIM_STATUS_HISTORY", sequenceName = "SEQ_CLAIM_STATUS_HISTORY", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CLAIM_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Claim claim;

    @Column(name = "PREVIOUS_STATUS")
    @Enumerated(EnumType.STRING)
    private Claim.ClaimStatus previousStatus;

    @Column(name = "NEW_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private Claim.ClaimStatus newStatus;

    @Column(name = "CHANGE_REASON", length = 1000)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;
}