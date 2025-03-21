package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestionar operaciones relacionadas con métodos de pago
 */
public interface PaymentMethodService {

    /**
     * Crea un nuevo método de pago
     */
    PaymentMethodDto createPaymentMethod(PaymentMethodDto paymentMethodDto);

    /**
     * Busca un método de pago por su ID
     */
    Optional<PaymentMethodDto> getPaymentMethodById(Long id);

    /**
     * Busca un método de pago por su número
     */
    Optional<PaymentMethodDto> getPaymentMethodByNumber(String paymentMethodNumber);

    /**
     * Busca métodos de pago para un cliente
     */
    List<PaymentMethodDto> getPaymentMethodsByCustomerNumber(String customerNumber);

    /**
     * Busca métodos de pago activos para un cliente
     */
    List<PaymentMethodDto> getActivePaymentMethodsByCustomerNumber(String customerNumber);

    /**
     * Obtiene el método de pago predeterminado de un cliente
     */
    Optional<PaymentMethodDto> getDefaultPaymentMethodByCustomerNumber(String customerNumber);

    /**
     * Busca métodos de pago por tipo
     */
    List<PaymentMethodDto> getPaymentMethodsByType(PaymentMethod.MethodType methodType);

    /**
     * Busca métodos de pago por término
     */
    Page<PaymentMethodDto> searchPaymentMethods(String searchTerm, Pageable pageable);

    /**
     * Actualiza un método de pago existente
     */
    PaymentMethodDto updatePaymentMethod(Long id, PaymentMethodDto paymentMethodDto);

    /**
     * Actualiza los datos de una tarjeta
     */
    PaymentMethodDto updateCardDetails(String paymentMethodNumber, String cardNumber, String cardExpiryMonth,
                                       String cardExpiryYear, String cvv);

    /**
     * Establece un método de pago como predeterminado
     */
    PaymentMethodDto setDefaultPaymentMethod(String paymentMethodNumber);

    /**
     * Activa un método de pago
     */
    PaymentMethodDto activatePaymentMethod(String paymentMethodNumber);

    /**
     * Desactiva un método de pago
     */
    PaymentMethodDto deactivatePaymentMethod(String paymentMethodNumber);

    /**
     * Valida un método de pago (realiza una transacción de prueba)
     */
    boolean validatePaymentMethod(String paymentMethodNumber);

    /**
     * Marca un método de pago como verificado
     */
    PaymentMethodDto markPaymentMethodAsVerified(String paymentMethodNumber);

    /**
     * Tokeniza los datos de un método de pago (para almacenamiento seguro)
     */
    String tokenizePaymentMethod(PaymentMethodDto paymentMethodDto);

    /**
     * Elimina un método de pago
     */
    void deletePaymentMethod(String paymentMethodNumber);

    /**
     * Busca tarjetas que expiran pronto
     */
    List<PaymentMethodDto> findCardsExpiringInMonth(int month, int year);

    /**
     * Actualiza masivamente métodos de pago expirados
     */
    CompletableFuture<Integer> updateExpiredPaymentMethods();

    /**
     * Busca y notifica sobre tarjetas próximas a expirar
     */
    CompletableFuture<List<PaymentMethodDto>> notifyCardsExpiringSoon(int daysAhead);
}