package com.insurtech.claim.model.dto;

import com.insurtech.claim.model.entity.Claim;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSearchRequestDto {

    private String claimNumber;
    private String policyNumber;
    private String customerNumber;
    private String customerEmail;
    private String identificationNumber;
    private String identificationType;
    private LocalDate incidentDateFrom;
    private LocalDate incidentDateTo;
    private List<Claim.ClaimStatus> statuses;
    private List<Claim.ClaimType> types;
    private Boolean highValue;
    private Integer daysOpen;
}