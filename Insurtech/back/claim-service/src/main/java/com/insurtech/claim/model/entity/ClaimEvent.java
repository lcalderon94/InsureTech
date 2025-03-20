package com.insurtech.claim.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM_EVENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClaimEvent {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIM_EVENTS")
    @SequenceGenerator(name = "SEQ_CLAIM_EVENTS", sequenceName = "SEQ_CLAIM_EVENTS", allocationSize = 1)
    private Long id;

    @Column(name = "CLAIM_ID", nullable = false)
    private Long claimId;

    @Column(name = "EVENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "DETAILS", length = 4000)
    private String details;

    @Column(name = "OLD_STATUS")
    @Enumerated(EnumType.STRING)
    private Claim.ClaimStatus oldStatus;

    @Column(name = "NEW_STATUS")
    @Enumerated(EnumType.STRING)
    private Claim.ClaimStatus newStatus;

    @Column(name = "EVENT_ID", nullable = false)
    private String eventId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    public enum EventType {
        CLAIM_CREATED, CLAIM_UPDATED, STATUS_CHANGED, DOCUMENT_ADDED, ITEM_ADDED,
        NOTE_ADDED, PAYMENT_ISSUED, INFORMATION_REQUESTED, ASSESSMENT_COMPLETED, DENIED
    }
}