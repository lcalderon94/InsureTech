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
public class BatchProcessingRequestDto {

    private List<Long> claimIds;
    private List<String> claimNumbers;
    private List<Claim.ClaimStatus> statuses;
    private Claim.ClaimStatus targetStatus;
    private LocalDate incidentDateFrom;
    private LocalDate incidentDateTo;
    private String processingReason;
    private String batchId;
}