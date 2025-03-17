package com.insurtech.customer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SEGMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Segment {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEGMENTS")
    @SequenceGenerator(name = "SEQ_SEGMENTS", sequenceName = "SEQ_SEGMENTS", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(name = "SEGMENT_TYPE")
    @Enumerated(EnumType.STRING)
    private SegmentType segmentType;

    @Column(name = "SEGMENT_CRITERIA")
    @Lob
    private String segmentCriteria;

    @Column(name = "IS_ACTIVE")
    private boolean isActive = true;

    @ManyToMany(mappedBy = "segments")
    @EqualsAndHashCode.Exclude
    private Set<Customer> customers = new HashSet<>();

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

    public enum SegmentType {
        DEMOGRAPHIC, BEHAVIORAL, GEOGRAPHIC, RISK_BASED, CUSTOM
    }
}
