package com.insurtech.policy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "POLICY_EVENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PolicyEvent {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICY_EVENTS")
    @SequenceGenerator(name = "SEQ_POLICY_EVENTS", sequenceName = "SEQ_POLICY_EVENTS", allocationSize = 1)
    private Long id;

    @Column(name = "POLICY_ID", nullable = false)
    private Long policyId;

    @Column(name = "EVENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "DETAILS", length = 4000)
    private String details;

    @Column(name = "OLD_STATUS")
    @Enumerated(EnumType.STRING)
    private Policy.PolicyStatus oldStatus;

    @Column(name = "NEW_STATUS")
    @Enumerated(EnumType.STRING)
    private Policy.PolicyStatus newStatus;

    @Column(name = "EVENT_ID", nullable = false)
    private String eventId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    public enum EventType {
        POLICY_CREATED, POLICY_UPDATED, POLICY_CANCELLED, POLICY_RENEWED,
        STATUS_CHANGED, COVERAGE_ADDED, COVERAGE_REMOVED, COVERAGE_UPDATED,
        DOCUMENT_ADDED, NOTE_ADDED, VERSION_CREATED
    }
}