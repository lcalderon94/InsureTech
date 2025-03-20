package com.insurtech.claim.config;

import com.insurtech.claim.batch.processor.ClaimItemProcessor;
import com.insurtech.claim.batch.reader.ClaimReader;
import com.insurtech.claim.batch.writer.ClaimWriter;
import com.insurtech.claim.batch.partitioner.ClaimPartitioner;
import com.insurtech.claim.model.entity.Claim;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TaskExecutor taskExecutor;

    @Value("${batch.claim.chunk-size:100}")
    private int chunkSize;

    @Value("${batch.claim.grid-size:4}")
    private int gridSize;

    @Bean
    public Partitioner claimPartitioner() {
        return new ClaimPartitioner();
    }

    @Bean
    @StepScope
    public ItemReader<Claim> claimReader(
            @Value("#{stepExecutionContext['minValue']}") Long minValue,
            @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {
        return new ClaimReader(dataSource, minValue, maxValue);
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
        return stepBuilderFactory.get("claimProcessingStep")
                .<Claim, Claim>chunk(chunkSize)
                .reader(claimReader(null, null))
                .processor(claimProcessor())
                .writer(claimWriter())
                .build();
    }

    @Bean
    public Step partitionStep() {
        return stepBuilderFactory.get("partitionStep")
                .partitioner("claimProcessingStep", claimPartitioner())
                .step(claimProcessingStep())
                .taskExecutor(taskExecutor)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Job processClaimsJob() {
        return jobBuilderFactory.get("processClaimsJob")
                .incrementer(new RunIdIncrementer())
                .flow(partitionStep())
                .end()
                .build();
    }
}