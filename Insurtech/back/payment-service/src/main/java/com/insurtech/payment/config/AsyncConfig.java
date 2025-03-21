package com.insurtech.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuración para ejecución asíncrona de tareas
 * Esta configuración es fundamental para el procesamiento paralelo de transacciones
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:25}")
    private int queueCapacity;

    /**
     * Configura el executor de tareas asíncronas
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("Creando Async Task Executor con corePoolSize {}, maxPoolSize {}, queueCapacity {}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("payment-async-");

        // Política de rechazo: cuando la cola está llena, el hilo que llama ejecutará la tarea
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("Cola de tareas llena, ejecutando en el hilo principal: {}", r.toString());
            if (!e.isShutdown()) {
                r.run();
            }
        });

        executor.initialize();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * Manejador de excepciones para tareas asíncronas
     */
    static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(CustomAsyncExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Excepción en ejecución de método asíncrono. Método: {}", method.getName(), ex);

            StringBuilder paramInfo = new StringBuilder();
            for (Object param : params) {
                paramInfo.append(param).append(", ");
            }

            log.error("Argumentos del método: {}", paramInfo);
        }
    }
}