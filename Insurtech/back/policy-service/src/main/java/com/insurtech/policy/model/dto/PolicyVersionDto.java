package com.insurtech.policy.model.dto;

import com.insurtech.policy.model.entity.Policy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyVersionDto {

    private Long id;

    private Long policyId;

    private Integer versionNumber;

    private Policy.PolicyStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal premium;

    private BigDecimal sumInsured;

    private String changeReason;

    private String policyData;

    private LocalDateTime createdAt;

    private String createdBy;
}