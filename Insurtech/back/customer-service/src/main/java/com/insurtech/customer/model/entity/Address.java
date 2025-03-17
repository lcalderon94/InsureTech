package com.insurtech.customer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADDRESSES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ADDRESSES")
    @SequenceGenerator(name = "SEQ_ADDRESSES", sequenceName = "SEQ_ADDRESSES", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @Column(name = "ADDRESS_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Column(nullable = false)
    private String street;

    @Column(name = "HOUSE_NUMBER")
    private String number;

    @Column
    private String unit;

    @Column
    private String city;

    @Column
    private String state;

    @Column(name = "POSTAL_CODE")
    private String postalCode;

    @Column
    private String country;

    @Column(name = "IS_PRIMARY")
    private boolean isPrimary;

    @Column(name = "IS_VALID")
    private boolean isValid;

    @Column
    private String latitude;

    @Column
    private String longitude;

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

    public enum AddressType {
        HOME, WORK, MAILING, BILLING, OTHER
    }
}