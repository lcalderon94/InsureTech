package com.insurtech.notification.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEvent extends BaseEvent {
    private UUID customerId;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String customerStatus; // ACTIVE, INACTIVE, PENDING_VERIFICATION
    private String actionType; // CREATED, UPDATED, VERIFIED, DEACTIVATED
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String zipCode;
    private String country;
    private Map<String, Object> additionalDetails;
}