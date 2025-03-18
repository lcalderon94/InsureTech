package com.insurtech.policy.model.dto;

import com.insurtech.policy.model.entity.Policy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDto {

    private Long id;

    private String policyNumber;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long customerId;

    private String customerNumber;

    @NotNull(message = "El tipo de póliza es obligatorio")
    private Policy.PolicyType policyType;

    private Policy.PolicyStatus status;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDate endDate;

    private LocalDate issueDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "La prima debe ser mayor que cero")
    private BigDecimal premium;

    @DecimalMin(value = "0.0", inclusive = false, message = "La suma asegurada debe ser mayor que cero")
    private BigDecimal sumInsured;

    private Policy.PaymentFrequency paymentFrequency;

    private Policy.PaymentMethod paymentMethod;

    private String description;

    @Valid
    private Set<PolicyCoverageDto> coverages = new HashSet<>();

    @Valid
    private Set<PolicyNoteDto> notes = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}