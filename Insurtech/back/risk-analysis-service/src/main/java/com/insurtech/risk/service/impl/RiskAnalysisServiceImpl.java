package com.insurtech.risk.service.impl;

import com.insurtech.risk.model.RiskEvaluation;
import com.insurtech.risk.repository.RiskRepository;
import com.insurtech.risk.service.RiskAnalysisService;
import com.insurtech.risk.service.async.RiskTask;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.springframework.stereotype.Service;

@Service
public class RiskAnalysisServiceImpl implements RiskAnalysisService {

    private final ForkJoinPool forkJoinPool;
    private final RiskRepository repository;

    public RiskAnalysisServiceImpl(ForkJoinPool forkJoinPool, RiskRepository repository) {
        this.forkJoinPool = forkJoinPool;
        this.repository = repository;
    }

    @Override
    public RiskEvaluation analyze(String policyNumber, List<Double> riskFactors) {
        double score = forkJoinPool.invoke(new RiskTask(riskFactors));
        RiskEvaluation evaluation = new RiskEvaluation(policyNumber, riskFactors, score);
        return repository.save(evaluation);
    }

    @Override
    public RiskEvaluation getById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<RiskEvaluation> findAll() {
        return repository.findAll();
    }
}
