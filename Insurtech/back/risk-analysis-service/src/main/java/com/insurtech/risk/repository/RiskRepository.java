package com.insurtech.risk.repository;

import com.insurtech.risk.model.RiskEvaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RiskRepository extends MongoRepository<RiskEvaluation, String> {
}
