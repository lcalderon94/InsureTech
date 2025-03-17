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
@Table(name = "PREFERENCES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Preference {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PREFERENCES")
    @SequenceGenerator(name = "SEQ_PREFERENCES", sequenceName = "SEQ_PREFERENCES", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Customer customer;

    @Column(name = "PREFERENCE_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PreferenceType preferenceType;

    @Column(name = "PREFERENCE_KEY", nullable = false)
    private String preferenceKey;

    @Column(name = "PREFERENCE_VALUE")
    private String preferenceValue;

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

    public enum PreferenceType {
        COMMUNICATION, MARKETING, NOTIFICATION, PRIVACY, LANGUAGE, UI, OTHER
    }
}
