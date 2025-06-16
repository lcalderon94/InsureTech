package com.insurtech.risk.service.async;

import java.util.List;
import java.util.concurrent.RecursiveTask;

public class RiskTask extends RecursiveTask<Double> {

    private static final int THRESHOLD = 10;
    private final List<Double> factors;

    public RiskTask(List<Double> factors) {
        this.factors = factors;
    }

    @Override
    protected Double compute() {
        if (factors.size() <= THRESHOLD) {
            return factors.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        int mid = factors.size() / 2;
        RiskTask left = new RiskTask(factors.subList(0, mid));
        RiskTask right = new RiskTask(factors.subList(mid, factors.size()));
        left.fork();
        double rightResult = right.compute();
        double leftResult = left.join();
        return (leftResult + rightResult) / 2;
    }
}
