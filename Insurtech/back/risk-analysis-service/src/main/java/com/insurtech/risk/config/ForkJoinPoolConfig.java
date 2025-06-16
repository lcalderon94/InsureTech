package com.insurtech.risk.config;

import java.util.concurrent.ForkJoinPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ForkJoinPoolConfig {

    @Value("${fork-join-pool.parallelism:2}")
    private int parallelism;

    @Value("${fork-join-pool.async-mode:true}")
    private boolean asyncMode;

    @Bean
    public ForkJoinPool forkJoinPool() {
        return new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, asyncMode);
    }
}
