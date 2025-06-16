package com.insurtech.risk.service;

import com.insurtech.risk.service.async.RiskTask;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiskTaskTest {

    @Test
    void computesAverageRisk() {
        ForkJoinPool pool = new ForkJoinPool();
        RiskTask task = new RiskTask(Arrays.asList(1.0, 2.0, 3.0, 4.0));
        double result = pool.invoke(task);
        assertEquals(2.5, result, 0.0001);
    }
}
