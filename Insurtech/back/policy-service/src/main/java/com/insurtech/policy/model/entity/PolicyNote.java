package com.insurtech.policy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "POLICY_NOTES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PolicyNote {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_POLICY_NOTES")
    @SequenceGenerator(name = "SEQ_POLICY_NOTES", sequenceName = "SEQ_POLICY_NOTES", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "POLICY_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @Column(name = "NOTE_TYPE")
    @Enumerated(EnumType.STRING)
    private NoteType noteType;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "CONTENT", nullable = false, length = 4000)
    private String content;

    @Column(name = "IS_IMPORTANT")
    private boolean isImportant;

    @Column(name = "IS_SYSTEM_GENERATED")
    private boolean isSystemGenerated;

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

    public enum NoteType {
        GENERAL, UNDERWRITING, CLAIM, BILLING, CUSTOMER_SERVICE, RENEWAL, CANCELLATION, OTHER
    }
}