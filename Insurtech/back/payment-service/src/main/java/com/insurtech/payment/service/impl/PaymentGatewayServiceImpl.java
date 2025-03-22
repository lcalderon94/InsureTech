package com.insurtech.payment.service.impl;

import com.insurtech.payment.event.producer.PaymentEventProducer;
import com.insurtech.payment.exception.TransactionFailedException;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.repository.TransactionRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementación del servicio de integración con pasarelas de pago
 * Esta implementación simula la interacción con pasarelas de pago externas
 * En un entorno real, se conectaría con APIs como Stripe, PayPal, etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final TransactionRepository transactionRepository;
    private final DistributedLockService lockService;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${payment.gateway.simulation.success-rate:90}")
    private int successRate;

    @Value("${payment.gateway.simulation.timeout-ms:500}")
    private int simulatedTimeoutMs;

    @Value("${payment.gateway.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * Procesa una transacción de pago
     */
    @Override
    public TransactionDto processPaymentTransaction(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata) {

        log.info("Procesando transacción de pago por {} {}", amount, currency);

        // Validar método de pago
        validatePaymentMethod(paymentMethodDto);

        String transactionId = UUID.randomUUID().toString();
        String lockKey = "transaction:" + transactionId;

        return lockService.executeWithLock(lockKey, () -> {
            try {
                // Simular tiempo de procesamiento de la pasarela de pago
                Thread.sleep(simulatedTimeoutMs);

                // Simular éxito/fallo basado en la tasa de éxito configurada
                boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

                TransactionDto transaction = new TransactionDto();
                transaction.setTransactionId(transactionId);
                transaction.setTransactionType(Transaction.TransactionType.PAYMENT);
                transaction.setAmount(amount);
                transaction.setCurrency(currency);
                transaction.setTransactionDate(LocalDateTime.now());

                if (successful) {
                    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
                    transaction.setGatewayReference("REF-" + UUID.randomUUID().toString().substring(0, 8));
                    transaction.setAuthorizationCode(generateAuthorizationCode());
                    transaction.setGatewayResponseCode("00");
                    transaction.setGatewayResponseMessage("Transacción aprobada");
                } else {
                    transaction.setStatus(Transaction.TransactionStatus.FAILED);
                    transaction.setErrorCode("DECLINED");
                    transaction.setErrorDescription("Transacción rechazada por la pasarela de pago");
                    transaction.setGatewayResponseCode("05");
                    transaction.setGatewayResponseMessage("Transacción rechazada");
                }

                log.info("Transacción {} procesada con estado: {}", transactionId, transaction.getStatus());

                return transaction;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransactionFailedException("Procesamiento interrumpido: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error al procesar transacción: {}", e.getMessage());
                throw new TransactionFailedException("Error al procesar la transacción: " + e.getMessage());
            }
        });
    }

    /**
     * Procesa una transacción de pago de forma asíncrona
     */
    @Override
    public CompletableFuture<TransactionDto> processPaymentTransactionAsync(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata) {

        return CompletableFuture.supplyAsync(() ->
                processPaymentTransaction(amount, currency, paymentMethodDto, description, metadata)
        );
    }

    /**
     * Autoriza una transacción sin capturarla
     */
    @Override
    public TransactionDto authorizeTransaction(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata) {

        log.info("Autorizando transacción por {} {}", amount, currency);

        // Validar método de pago
        validatePaymentMethod(paymentMethodDto);

        String transactionId = UUID.randomUUID().toString();
        String lockKey = "transaction_auth:" + transactionId;

        return lockService.executeWithLock(lockKey, () -> {
            try {
                // Simular tiempo de procesamiento
                Thread.sleep(simulatedTimeoutMs);

                // Simular éxito/fallo basado en la tasa de éxito configurada
                boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

                TransactionDto transaction = new TransactionDto();
                transaction.setTransactionId(transactionId);
                transaction.setTransactionType(Transaction.TransactionType.AUTH_ONLY);
                transaction.setAmount(amount);
                transaction.setCurrency(currency);
                transaction.setTransactionDate(LocalDateTime.now());

                if (successful) {
                    transaction.setStatus(Transaction.TransactionStatus.AUTHORIZED);
                    transaction.setGatewayReference("AUTH-" + UUID.randomUUID().toString().substring(0, 8));
                    transaction.setAuthorizationCode(generateAuthorizationCode());
                    transaction.setGatewayResponseCode("00");
                    transaction.setGatewayResponseMessage("Autorización aprobada");
                } else {
                    transaction.setStatus(Transaction.TransactionStatus.FAILED);
                    transaction.setErrorCode("AUTH_DECLINED");
                    transaction.setErrorDescription("Autorización rechazada por la pasarela de pago");
                    transaction.setGatewayResponseCode("05");
                    transaction.setGatewayResponseMessage("Autorización rechazada");
                }

                log.info("Autorización {} procesada con estado: {}", transactionId, transaction.getStatus());

                return transaction;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransactionFailedException("Autorización interrumpida: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error al autorizar transacción: {}", e.getMessage());
                throw new TransactionFailedException("Error al autorizar la transacción: " + e.getMessage());
            }
        });
    }

    /**
     * Captura una transacción previamente autorizada
     */
    @Override
    public TransactionDto captureTransaction(String authorizationId, BigDecimal amount) {
        log.info("Capturando transacción autorizada: {}", authorizationId);

        String transactionId = UUID.randomUUID().toString();
        String lockKey = "transaction_capture:" + authorizationId;

        return lockService.executeWithLock(lockKey, () -> {
            try {
                // Simular tiempo de procesamiento
                Thread.sleep(simulatedTimeoutMs);

                // Simular éxito/fallo basado en la tasa de éxito configurada
                boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

                TransactionDto transaction = new TransactionDto();
                transaction.setTransactionId(transactionId);
                transaction.setTransactionType(Transaction.TransactionType.CAPTURE);
                transaction.setAmount(amount);
                transaction.setTransactionDate(LocalDateTime.now());

                if (successful) {
                    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
                    transaction.setGatewayReference("CAP-" + UUID.randomUUID().toString().substring(0, 8));
                    transaction.setAuthorizationCode(generateAuthorizationCode());
                    transaction.setGatewayResponseCode("00");
                    transaction.setGatewayResponseMessage("Captura aprobada");
                } else {
                    transaction.setStatus(Transaction.TransactionStatus.FAILED);
                    transaction.setErrorCode("CAPTURE_DECLINED");
                    transaction.setErrorDescription("Captura rechazada por la pasarela de pago");
                    transaction.setGatewayResponseCode("05");
                    transaction.setGatewayResponseMessage("Captura rechazada");
                }

                log.info("Captura {} procesada con estado: {}", transactionId, transaction.getStatus());

                return transaction;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransactionFailedException("Captura interrumpida: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error al capturar transacción: {}", e.getMessage());
                throw new TransactionFailedException("Error al capturar la transacción: " + e.getMessage());
            }
        });
    }

    /**
     * Procesa un reembolso
     */
    @Override
    public TransactionDto processRefund(
            String originalTransactionId,
            BigDecimal amount,
            String currency,
            String reason,
            Map<String, String> metadata) {

        log.info("Procesando reembolso por {} {} para transacción original: {}",
                amount, currency, originalTransactionId);

        String transactionId = UUID.randomUUID().toString();
        String lockKey = "transaction_refund:" + originalTransactionId;

        return lockService.executeWithLock(lockKey, () -> {
            try {
                // Simular tiempo de procesamiento
                Thread.sleep(simulatedTimeoutMs);

                // Simular éxito/fallo basado en la tasa de éxito configurada
                boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

                TransactionDto transaction = new TransactionDto();
                transaction.setTransactionId(transactionId);
                transaction.setTransactionType(Transaction.TransactionType.REFUND);
                transaction.setAmount(amount);
                transaction.setCurrency(currency);
                transaction.setTransactionDate(LocalDateTime.now());

                if (successful) {
                    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
                    transaction.setGatewayReference("REFUND-" + UUID.randomUUID().toString().substring(0, 8));
                    transaction.setGatewayResponseCode("00");
                    transaction.setGatewayResponseMessage("Reembolso aprobado");
                } else {
                    transaction.setStatus(Transaction.TransactionStatus.FAILED);
                    transaction.setErrorCode("REFUND_DECLINED");
                    transaction.setErrorDescription("Reembolso rechazado por la pasarela de pago");
                    transaction.setGatewayResponseCode("05");
                    transaction.setGatewayResponseMessage("Reembolso rechazado");
                }

                log.info("Reembolso {} procesado con estado: {}", transactionId, transaction.getStatus());

                return transaction;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransactionFailedException("Reembolso interrumpido: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error al procesar reembolso: {}", e.getMessage());
                throw new TransactionFailedException("Error al procesar el reembolso: " + e.getMessage());
            }
        });
    }

    /**
     * Anula una transacción
     */
    @Override
    public TransactionDto voidTransaction(String transactionId, String reason) {
        log.info("Anulando transacción: {}", transactionId);

        String voidTransactionId = UUID.randomUUID().toString();
        String lockKey = "transaction_void:" + transactionId;

        return lockService.executeWithLock(lockKey, () -> {
            try {
                // Simular tiempo de procesamiento
                Thread.sleep(simulatedTimeoutMs);

                // Simular éxito/fallo basado en la tasa de éxito configurada
                boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

                TransactionDto transaction = new TransactionDto();
                transaction.setTransactionId(voidTransactionId);
                transaction.setTransactionType(Transaction.TransactionType.VOID);
                transaction.setTransactionDate(LocalDateTime.now());

                if (successful) {
                    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
                    transaction.setGatewayReference("VOID-" + UUID.randomUUID().toString().substring(0, 8));
                    transaction.setGatewayResponseCode("00");
                    transaction.setGatewayResponseMessage("Anulación aprobada");
                } else {
                    transaction.setStatus(Transaction.TransactionStatus.FAILED);
                    transaction.setErrorCode("VOID_DECLINED");
                    transaction.setErrorDescription("Anulación rechazada por la pasarela de pago");
                    transaction.setGatewayResponseCode("05");
                    transaction.setGatewayResponseMessage("Anulación rechazada");
                }

                log.info("Anulación {} procesada con estado: {}", voidTransactionId, transaction.getStatus());

                return transaction;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransactionFailedException("Anulación interrumpida: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error al anular transacción: {}", e.getMessage());
                throw new TransactionFailedException("Error al anular la transacción: " + e.getMessage());
            }
        });
    }

    /**
     * Verifica el estado de una transacción
     */
    @Override
    public Transaction.TransactionStatus checkTransactionStatus(String transactionId) {
        log.info("Verificando estado de transacción: {}", transactionId);

        // Buscar transacción en base de datos
        return transactionRepository.findByTransactionId(transactionId)
                .map(Transaction::getStatus)
                .orElse(Transaction.TransactionStatus.PENDING);
    }

    /**
     * Tokeniza un método de pago para uso futuro
     */
    @Override
    public String tokenizePaymentMethod(PaymentMethodDto paymentMethodDto) {
        log.info("Tokenizando método de pago para cliente: {}", paymentMethodDto.getCustomerNumber());

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 2);

            // Generar token (en un entorno real, esto usaría el servicio de tokenización del proveedor de pagos)
            String token = "tok_" + UUID.randomUUID().toString().replace("-", "");

            log.info("Método de pago tokenizado exitosamente: {}", token);

            return token;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Tokenización interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al tokenizar método de pago: {}", e.getMessage());
            throw new TransactionFailedException("Error al tokenizar método de pago: " + e.getMessage());
        }
    }

    /**
     * Crea un cliente en la pasarela de pago
     */
    @Override
    public String createCustomerProfile(String customerNumber, String email, String name) {
        log.info("Creando perfil de cliente en pasarela de pago: {}", customerNumber);

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 2);

            // Generar ID de perfil (en un entorno real, esto usaría la API del proveedor de pagos)
            String profileId = "cus_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

            log.info("Perfil de cliente creado exitosamente: {}", profileId);

            return profileId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Creación de perfil interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al crear perfil de cliente: {}", e.getMessage());
            throw new TransactionFailedException("Error al crear perfil de cliente: " + e.getMessage());
        }
    }

    /**
     * Asocia un método de pago a un cliente
     */
    @Override
    public String attachPaymentMethodToCustomer(String customerProfileId, PaymentMethodDto paymentMethodDto) {
        log.info("Asociando método de pago a cliente: {}", customerProfileId);

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 2);

            // Generar ID de método asociado
            String attachmentId = "pm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

            log.info("Método de pago asociado exitosamente: {}", attachmentId);

            return attachmentId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Asociación interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al asociar método de pago: {}", e.getMessage());
            throw new TransactionFailedException("Error al asociar método de pago: " + e.getMessage());
        }
    }

    /**
     * Configura pagos recurrentes
     */
    @Override
    public String setupRecurringPayment(
            String customerProfileId,
            String paymentMethodToken,
            BigDecimal amount,
            String currency,
            String frequency,
            int totalPayments,
            String description) {

        log.info("Configurando pago recurrente para cliente: {}", customerProfileId);

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs);

            // Generar ID de suscripción
            String subscriptionId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

            log.info("Pago recurrente configurado exitosamente: {}", subscriptionId);

            return subscriptionId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Configuración interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al configurar pago recurrente: {}", e.getMessage());
            throw new TransactionFailedException("Error al configurar pago recurrente: " + e.getMessage());
        }
    }

    /**
     * Cancela un plan de pagos recurrentes
     */
    @Override
    public boolean cancelRecurringPayment(String recurringPaymentId, String reason) {
        log.info("Cancelando pago recurrente: {}", recurringPaymentId);

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 2);

            // Simular éxito/fallo
            boolean successful = ThreadLocalRandom.current().nextInt(100) < successRate;

            if (successful) {
                log.info("Pago recurrente cancelado exitosamente: {}", recurringPaymentId);
            } else {
                log.warn("No se pudo cancelar el pago recurrente: {}", recurringPaymentId);
            }

            return successful;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Cancelación interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al cancelar pago recurrente: {}", e.getMessage());
            throw new TransactionFailedException("Error al cancelar pago recurrente: " + e.getMessage());
        }
    }

    /**
     * Verifica si un método de pago es válido
     */
    @Override
    public boolean validatePaymentMethod(PaymentMethodDto paymentMethodDto) {
        log.info("Validando método de pago: {}", paymentMethodDto.getPaymentMethodNumber());

        try {
            // Validar que se proporcionen los datos mínimos necesarios según el tipo
            if (paymentMethodDto.getMethodType() == null) {
                throw new IllegalArgumentException("El tipo de método de pago es obligatorio");
            }

            switch (paymentMethodDto.getMethodType()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    // Validar campos de tarjeta
                    validateCardDetails(paymentMethodDto);
                    break;
                case BANK_ACCOUNT:
                    // Validar campos de cuenta bancaria
                    if (paymentMethodDto.getAccountNumber() == null || paymentMethodDto.getAccountNumber().isEmpty()) {
                        throw new IllegalArgumentException("El número de cuenta es obligatorio");
                    }
                    break;
                case ELECTRONIC_WALLET:
                    // Validar campos de monedero electrónico
                    if (paymentMethodDto.getWalletProvider() == null || paymentMethodDto.getWalletProvider().isEmpty()) {
                        throw new IllegalArgumentException("El proveedor del monedero es obligatorio");
                    }
                    break;
                default:
                    // Otros tipos
                    break;
            }

            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 3);

            // Simular validación exitosa en la mayoría de los casos
            boolean valid = ThreadLocalRandom.current().nextInt(100) < 95;

            if (valid) {
                log.info("Método de pago validado exitosamente");
            } else {
                log.warn("Método de pago inválido");
            }

            return valid;

        } catch (IllegalArgumentException e) {
            log.warn("Validación de método de pago fallida: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Validación interrumpida");
            return false;
        } catch (Exception e) {
            log.error("Error al validar método de pago: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene datos detallados de una transacción
     */
    @Override
    public Map<String, Object> getTransactionDetails(String transactionId) {
        log.info("Obteniendo detalles de transacción: {}", transactionId);

        try {
            // Simular tiempo de procesamiento
            Thread.sleep(simulatedTimeoutMs / 2);

            // Buscar transacción en base de datos
            return transactionRepository.findByTransactionId(transactionId)
                    .map(transaction -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("transactionId", transaction.getTransactionId());
                        details.put("type", transaction.getTransactionType());
                        details.put("status", transaction.getStatus());
                        details.put("amount", transaction.getAmount());
                        details.put("currency", transaction.getCurrency());
                        details.put("date", transaction.getTransactionDate());
                        details.put("gatewayReference", transaction.getGatewayReference());
                        details.put("authorizationCode", transaction.getAuthorizationCode());

                        if (transaction.getStatus() == Transaction.TransactionStatus.FAILED) {
                            details.put("errorCode", transaction.getErrorCode());
                            details.put("errorDescription", transaction.getErrorDescription());
                        }

                        return details;
                    })
                    .orElseGet(() -> {
                        Map<String, Object> notFound = new HashMap<>();
                        notFound.put("error", "Transacción no encontrada");
                        return notFound;
                    });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionFailedException("Consulta interrumpida: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al obtener detalles de transacción: {}", e.getMessage());
            throw new TransactionFailedException("Error al obtener detalles de transacción: " + e.getMessage());
        }
    }

    // Métodos privados de ayuda

    /**
     * Valida los datos de una tarjeta
     */
    private void validateCardDetails(PaymentMethodDto paymentMethodDto) {
        // Validar si hay un token (datos tokenizados)
        if (paymentMethodDto.getPaymentToken() != null && !paymentMethodDto.getPaymentToken().isEmpty()) {
            return;
        }

        // Si no hay token, validar campos detallados
        if (paymentMethodDto.getFullCardNumber() != null) {
            // Validar número de tarjeta
            String cardNumber = paymentMethodDto.getFullCardNumber().replaceAll("\\s|-", "");
            if (!isValidCardNumber(cardNumber)) {
                throw new IllegalArgumentException("Número de tarjeta inválido");
            }
        } else if (paymentMethodDto.getMaskedCardNumber() == null || paymentMethodDto.getMaskedCardNumber().isEmpty()) {
            throw new IllegalArgumentException("Se requiere número de tarjeta o token");
        }

        // Validar fecha de expiración
        if (paymentMethodDto.getCardExpiryDate() != null) {
            if (paymentMethodDto.getCardExpiryDate().isBefore(YearMonth.now())) {
                throw new IllegalArgumentException("La tarjeta ha expirado");
            }
        }
    }

    /**
     * Valida un número de tarjeta usando el algoritmo de Luhn
     */
    private boolean isValidCardNumber(String cardNumber) {
        // Simulamos la validación - en producción usaríamos el algoritmo de Luhn
        return cardNumber != null &&
                cardNumber.matches("\\d{13,19}") &&
                ThreadLocalRandom.current().nextInt(100) < 95;
    }

    /**
     * Genera un código de autorización aleatorio
     */
    private String generateAuthorizationCode() {
        // Generar un código de 6 caracteres alfanuméricos
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}