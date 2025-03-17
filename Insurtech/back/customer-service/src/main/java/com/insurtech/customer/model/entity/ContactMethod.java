package com.insurtech.customer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CONTACT_METHODS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CONTACT_METHODS")
    @SequenceGenerator(name = "SEQ_CONTACT_METHODS", sequenceName = "SEQ_CONTACT_METHODS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @Column(name = "CONTACT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    @Column(name = "CONTACT_VALUE", nullable = false)
    private String contactValue;

    @Column(name = "IS_PRIMARY")
    private boolean isPrimary;

    @Column(name = "IS_VERIFIED")
    private boolean isVerified;

    @Column(name = "IS_OPTED_IN")
    private boolean isOptedIn;

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

    public enum ContactType {
        EMAIL, MOBILE_PHONE, HOME_PHONE, WORK_PHONE, FAX, SOCIAL_MEDIA, OTHER
    }
}