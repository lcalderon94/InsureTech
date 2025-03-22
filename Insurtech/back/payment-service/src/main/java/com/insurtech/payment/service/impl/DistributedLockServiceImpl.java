package com.insurtech.payment.service.impl;

import com.insurtech.payment.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final JdbcLockRegistry lockRegistry;
    private static final long DEFAULT_TIMEOUT = 10; // 10 segundos por defecto
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT, operation);
    }

    @Override
    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation) {
        Lock lock = lockRegistry.obtain(lockKey);
        try {
            boolean acquired = lock.tryLock(timeout, timeUnit);
            if (!acquired) {
                throw new IllegalStateException("No se pudo adquirir el bloqueo para la clave: " + lockKey);
            }
            log.debug("Bloqueo adquirido para la clave: {}", lockKey);
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupción al intentar adquirir el bloqueo para la clave: " + lockKey, e);
        } finally {
            try {
                lock.unlock();
                log.debug("Bloqueo liberado para la clave: {}", lockKey);
            } catch (Exception e) {
                log.warn("Error al liberar el bloqueo para la clave: {}", lockKey, e);
            }
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, long leaseTime, TimeUnit leaseTimeUnit, Supplier<T> operation) {
        return executeWithLock(lockKey, timeout, timeUnit, operation);
    }

    @Override
    public boolean isLocked(String lockKey) {
        Lock lock = lockRegistry.obtain(lockKey);
        boolean canLock = false;
        try {
            canLock = lock.tryLock(0, TimeUnit.MILLISECONDS);
            return !canLock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        } finally {
            if (canLock) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean forceLockRelease(String lockKey) {
        Lock lock = lockRegistry.obtain(lockKey);
        try {
            lock.unlock();
            return true;
        } catch (Exception e) {
            log.error("No se pudo forzar la liberación del bloqueo para la clave: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public <T> T executeWithMultiLock(List<String> lockKeys, Supplier<T> operation) {
        if (lockKeys.isEmpty()) {
            return operation.get();
        }

        // Ordenar las claves para evitar deadlocks
        lockKeys.sort(String::compareTo);

        // Adquirir todos los bloqueos en orden
        for (String key : lockKeys) {
            Lock lock = lockRegistry.obtain(key);
            try {
                boolean acquired = lock.tryLock(DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
                if (!acquired) {
                    throw new IllegalStateException("No se pudo adquirir el bloqueo para la clave: " + key);
                }
                log.debug("Bloqueo adquirido para la clave: {}", key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupción al intentar adquirir el bloqueo para la clave: " + key, e);
            }
        }

        try {
            return operation.get();
        } finally {
            // Liberar los bloqueos en orden inverso
            for (int i = lockKeys.size() - 1; i >= 0; i--) {
                String key = lockKeys.get(i);
                Lock lock = lockRegistry.obtain(key);
                try {
                    lock.unlock();
                    log.debug("Bloqueo liberado para la clave: {}", key);
                } catch (Exception e) {
                    log.warn("Error al liberar el bloqueo para la clave: {}", key, e);
                }
            }
        }
    }
}