package com.insurtech.claim.config;

import com.insurtech.claim.batch.processor.ClaimItemProcessor;
import com.insurtech.claim.batch.reader.ClaimReader;
import com.insurtech.claim.batch.writer.ClaimWriter;
import com.insurtech.claim.batch.partitioner.ClaimPartitioner;
import com.insurtech.claim.model.entity.Claim;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.batch.job.enabled", havingValue = "true", matchIfMissing = false)
public class BatchConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Value("${batch.claim.chunk-size:100}")
    private int chunkSize;

    @Value("${batch.claim.grid-size:4}")
    private int gridSize;

    @Bean
    public PlatformTransactionManager batchTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ClaimPartitioner claimPartitioner() {
        return new ClaimPartitioner();
    }

    @Bean
    public ItemProcessor<Claim, Claim> claimProcessor() {
        return new ClaimItemProcessor();
    }

    @Bean
    public ItemWriter<Claim> claimWriter() {
        return new ClaimWriter();
    }

    @Bean
    public Step claimProcessingStep() {
        return new StepBuilder("claimProcessingStep", jobRepository)
                .<Claim, Claim>chunk(chunkSize, batchTransactionManager())
                .reader(new ClaimReader(dataSource, 1L, 100L)) // Valores por defecto
                .processor(claimProcessor())
                .writer(claimWriter())
                .build();
    }

    @Bean
    public Step partitionStep() {
        return new StepBuilder("partitionStep", jobRepository)
                .partitioner("claimProcessingStep", claimPartitioner())
                .step(claimProcessingStep())
                .taskExecutor(batchTaskExecutor())
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Job processClaimsJob() {
        return new JobBuilder("processClaimsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(partitionStep())
                .end()
                .build();
    }
}