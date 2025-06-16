package com.insurtech.risk.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "riskEvaluations")
public class RiskEvaluation {

    @Id
    private String id;
    private String policyNumber;
    private List<Double> riskFactors;
    private double riskScore;
    private LocalDateTime analysisDate = LocalDateTime.now();

    public RiskEvaluation() {
    }

    public RiskEvaluation(String policyNumber, List<Double> riskFactors, double riskScore) {
        this.policyNumber = policyNumber;
        this.riskFactors = riskFactors;
        this.riskScore = riskScore;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public List<Double> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<Double> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(LocalDateTime analysisDate) {
        this.analysisDate = analysisDate;
    }
}
