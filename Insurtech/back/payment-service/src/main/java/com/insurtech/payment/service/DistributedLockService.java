package com.insurtech.payment.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Servicio para gestionar bloqueos distribuidos
 *
 * Este servicio permite operaciones seguras en entornos distribuidos donde
 * múltiples instancias pueden intentar modificar los mismos recursos.
 */
public interface DistributedLockService {

    /**
     * Adquiere un bloqueo y ejecuta la operación si tiene éxito
     *
     * @param lockKey Clave única para el bloqueo
     * @param operation Operación a ejecutar si se adquiere el bloqueo
     * @param <T> Tipo de retorno de la operación
     * @return Resultado de la operación o null si no se pudo adquirir el bloqueo
     */
    <T> T executeWithLock(String lockKey, Supplier<T> operation);

    /**
     * Adquiere un bloqueo y ejecuta la operación si tiene éxito, con timeout personalizado
     *
     * @param lockKey Clave única para el bloqueo
     * @param timeout Tiempo máximo de espera para adquirir el bloqueo
     * @param timeUnit Unidad de tiempo para el timeout
     * @param operation Operación a ejecutar si se adquiere el bloqueo
     * @param <T> Tipo de retorno de la operación
     * @return Resultado de la operación o null si no se pudo adquirir el bloqueo
     */
    <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Supplier<T> operation);

    /**
     * Adquiere un bloqueo y ejecuta la operación si tiene éxito, con tiempo de retención del bloqueo
     *
     * @param lockKey Clave única para el bloqueo
     * @param timeout Tiempo máximo de espera para adquirir el bloqueo
     * @param timeUnit Unidad de tiempo para el timeout
     * @param leaseTime Tiempo que se mantendrá el bloqueo
     * @param leaseTimeUnit Unidad de tiempo para el lease time
     * @param operation Operación a ejecutar si se adquiere el bloqueo
     * @param <T> Tipo de retorno de la operación
     * @return Resultado de la operación o null si no se pudo adquirir el bloqueo
     */
    <T> T executeWithLock(String lockKey, long timeout, TimeUnit timeUnit,
                          long leaseTime, TimeUnit leaseTimeUnit, Supplier<T> operation);

    /**
     * Verifica si un bloqueo está actualmente adquirido
     *
     * @param lockKey Clave única del bloqueo
     * @return true si el bloqueo existe, false en caso contrario
     */
    boolean isLocked(String lockKey);

    /**
     * Fuerza la liberación de un bloqueo (usar con precaución)
     *
     * @param lockKey Clave única del bloqueo
     * @return true si el bloqueo fue liberado, false en caso contrario
     */
    boolean forceLockRelease(String lockKey);

    /**
     * Adquiere múltiples bloqueos y ejecuta la operación si tiene éxito
     *
     * @param lockKeys Lista de claves únicas para los bloqueos
     * @param operation Operación a ejecutar si se adquieren todos los bloqueos
     * @param <T> Tipo de retorno de la operación
     * @return Resultado de la operación o null si no se pudieron adquirir los bloqueos
     */
    <T> T executeWithMultiLock(List<String> lockKeys, Supplier<T> operation);
}