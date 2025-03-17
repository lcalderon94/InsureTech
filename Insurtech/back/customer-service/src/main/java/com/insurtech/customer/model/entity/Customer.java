package com.insurtech.customer.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CUSTOMERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CUSTOMERS")
    @SequenceGenerator(name = "SEQ_CUSTOMERS", sequenceName = "SEQ_CUSTOMERS", allocationSize = 1)
    private Long id;

    @Column(name = "CUSTOMER_NUMBER", unique = true, nullable = false)
    private String customerNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String identificationNumber;

    @Column(name = "IDENTIFICATION_TYPE")
    private String identificationType;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDateTime dateOfBirth;

    @Column
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "CUSTOMER_STATUS")
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "RISK_PROFILE")
    private String riskProfile;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContactMethod> contactMethods = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Preference> preferences = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "CUSTOMER_SEGMENTS",
            joinColumns = @JoinColumn(name = "CUSTOMER_ID"),
            inverseJoinColumns = @JoinColumn(name = "SEGMENT_ID")
    )
    private Set<Segment> segments = new HashSet<>();

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

    // Métodos para gestionar relaciones bidireccionales
    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setCustomer(null);
    }

    public void addContactMethod(ContactMethod contactMethod) {
        contactMethods.add(contactMethod);
        contactMethod.setCustomer(this);
    }

    public void removeContactMethod(ContactMethod contactMethod) {
        contactMethods.remove(contactMethod);
        contactMethod.setCustomer(null);
    }

    public void addPreference(Preference preference) {
        preferences.add(preference);
        preference.setCustomer(this);
    }

    public void removePreference(Preference preference) {
        preferences.remove(preference);
        preference.setCustomer(null);
    }

    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED, CLOSED
    }
}