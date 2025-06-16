package com.insurtech.risk.service;

import com.insurtech.risk.model.RiskEvaluation;
import java.util.List;

public interface RiskAnalysisService {
    RiskEvaluation analyze(String policyNumber, List<Double> riskFactors);
    RiskEvaluation getById(String id);
    List<RiskEvaluation> findAll();
}
