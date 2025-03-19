package com.insurtech.quote.model.dto;

import com.insurtech.quote.model.entity.Quote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponseDto {

    private String quoteNumber;

    private Quote.QuoteType quoteType;

    private Quote.QuoteStatus status;

    private BigDecimal totalPremium;

    private BigDecimal totalSumInsured;

    private String customerName;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Quote.PaymentFrequency paymentFrequency;

    private List<QuoteOptionDto> options = new ArrayList<>();

    private LocalDateTime validUntil;

    private LocalDateTime createdAt;
}