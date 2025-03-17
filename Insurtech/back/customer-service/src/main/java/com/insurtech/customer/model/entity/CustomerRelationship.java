package com.insurtech.customer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_RELATIONSHIPS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustomerRelationship {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CUSTOMER_RELATIONSHIPS")
    @SequenceGenerator(name = "SEQ_CUSTOMER_RELATIONSHIPS", sequenceName = "SEQ_CUSTOMER_RELATIONSHIPS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PRIMARY_CUSTOMER_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Customer primaryCustomer;

    @ManyToOne
    @JoinColumn(name = "RELATED_CUSTOMER_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Customer relatedCustomer;

    @Column(name = "RELATIONSHIP_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType;

    @Column
    private String description;

    @Column(name = "IS_ACTIVE")
    private boolean isActive = true;

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

    public enum RelationshipType {
        SPOUSE, PARENT, CHILD, SIBLING, GUARDIAN, BUSINESS_PARTNER, OTHER
    }
}
