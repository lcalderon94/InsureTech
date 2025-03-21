package com.insurtech.claim.batch.partitioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class ClaimPartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(ClaimPartitioner.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        log.info("Particionando job de procesamiento de reclamaciones en {} particiones", gridSize);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Consultas específicas para Oracle
        Long minId = jdbcTemplate.queryForObject(
                "SELECT NVL(MIN(id), 1) FROM CLAIMS", Long.class);
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT NVL(MAX(id), 1) FROM CLAIMS", Long.class);

        // Calcular el tamaño de cada partición
        long targetSize = (maxId - minId) / gridSize + 1;

        log.info("Datos a particionar: minId={}, maxId={}, targetSize={}", minId, maxId, targetSize);

        Map<String, ExecutionContext> result = new HashMap<>();

        long number = 0;
        long start = minId;
        long end = Math.min(start + targetSize - 1, maxId);

        // Crear particiones
        while (start <= maxId) {
            ExecutionContext context = new ExecutionContext();
            result.put("partition" + number, context);

            context.putLong("minValue", start);
            context.putLong("maxValue", end);

            log.debug("Creada partición: partition{}, minValue={}, maxValue={}", number, start, end);

            start += targetSize;
            end = Math.min(start + targetSize - 1, maxId);
            number++;
        }

        log.info("Creadas {} particiones", result.size());
        return result;
    }
}