package com.insurtech.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import java.util.function.Function;


import javax.sql.DataSource;

/**
 * Configuración para el sistema de bloqueos distribuidos
 *
 * Esta configuración es esencial para evitar condiciones de carrera
 * en transacciones financieras procesadas en múltiples instancias
 */
@Configuration
public class DistributedLockConfig {

    /**
     * Repositorio de bloqueos que utiliza la base de datos Oracle
     */
    @Bean
    public LockRepository lockRepository(DataSource dataSource) {
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
        lockRepository.setTimeToLive(30000); // TTL de 30 segundos para prevenir bloqueos muertos
        lockRepository.setPrefix("PAYMENT_LOCK_");
        return lockRepository;
    }

    /**
     * Registro de bloqueos que utiliza el repositorio definido
     */
    @Bean
    public JdbcLockRegistry lockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }
}