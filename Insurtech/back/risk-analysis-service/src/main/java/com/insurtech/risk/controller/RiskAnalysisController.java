package com.insurtech.risk.controller;

import com.insurtech.risk.model.RiskEvaluation;
import com.insurtech.risk.service.RiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/risk")
@Tag(name = "Risk Analysis API")
public class RiskAnalysisController {

    private final RiskAnalysisService service;

    public RiskAnalysisController(RiskAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "Start risk analysis")
    @PostMapping("/analyze")
    public ResponseEntity<RiskEvaluation> analyze(@RequestBody RiskRequest request) {
        RiskEvaluation evaluation = service.analyze(request.getPolicyNumber(), request.getRiskFactors());
        return ResponseEntity.ok(evaluation);
    }

    @Operation(summary = "Get evaluation by id")
    @GetMapping("/{id}")
    public ResponseEntity<RiskEvaluation> getById(@PathVariable String id) {
        RiskEvaluation evaluation = service.getById(id);
        if (evaluation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(evaluation);
    }

    @Operation(summary = "List evaluations")
    @GetMapping
    public List<RiskEvaluation> findAll() {
        return service.findAll();
    }

    public static class RiskRequest {
        private String policyNumber;
        private List<Double> riskFactors;

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
    }
}
