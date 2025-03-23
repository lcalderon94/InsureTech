package com.insurtech.payment.service.impl;

import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.client.PolicyServiceClient;
import com.insurtech.payment.exception.PaymentProcessingException;
import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.RefundDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.model.entity.Refund;
import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.repository.PaymentMethodRepository;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.repository.RefundRepository;
import com.insurtech.payment.repository.TransactionRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentGatewayService;
import com.insurtech.payment.service.RefundService;
import com.insurtech.payment.util.EntityDtoMapper;
import com.insurtech.payment.util.PaymentNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;
    private final EntityDtoMapper mapper;
    private final PaymentNumberGenerator numberGenerator;
    private final PaymentGatewayService paymentGatewayService;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CustomerServiceClient customerServiceClient;
    private final PolicyServiceClient policyServiceClient;

    @Override
    @Transactional
    public RefundDto requestRefund(RefundDto refundDto) {
        log.info("Solicitando reembolso para cliente número: {}", refundDto.getCustomerNumber());

        try {
            Map<String, Object> customer = customerServiceClient.getCustomerByNumber(refundDto.getCustomerNumber());
            Long customerId = ((Number) customer.get("id")).longValue();
            log.debug("Cliente resuelto con ID: {}", customerId);
        } catch (Exception e) {
            log.error("Error al resolver cliente por número: {}", refundDto.getCustomerNumber(), e);
            throw new ResourceNotFoundException("Cliente no encontrado con número: " + refundDto.getCustomerNumber());
        }

        // Validación de póliza si se proporciona
        if (refundDto.getPolicyNumber() != null && !refundDto.getPolicyNumber().isEmpty()) {
            try {
                Map<String, Object> policy = policyServiceClient.getPolicyByNumber(refundDto.getPolicyNumber());
                Long policyId = ((Number) policy.get("id")).longValue();
                log.debug("Póliza resuelta con ID: {}", policyId);
            } catch (Exception e) {
                log.error("Error al resolver póliza por número: {}", refundDto.getPolicyNumber(), e);
                throw new ResourceNotFoundException("Póliza no encontrada con número: " + refundDto.getPolicyNumber());
            }
        }

        // Validar pago original si se proporciona
        Payment originalPayment = null;
        if (refundDto.getOriginalPaymentNumber() != null) {
            originalPayment = paymentRepository.findByPaymentNumber(refundDto.getOriginalPaymentNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pago original no encontrado con número: " + refundDto.getOriginalPaymentNumber()));

            // Validar que el pago esté completado
            if (originalPayment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                throw new PaymentProcessingException("Solo se pueden reembolsar pagos completados");
            }

            // Validar que el monto a reembolsar no exceda el pago original
            if (refundDto.getAmount().compareTo(originalPayment.getAmount()) > 0) {
                throw new PaymentProcessingException(
                        "El monto del reembolso no puede ser mayor que el pago original");
            }
        }

        // Validar método de pago si se proporciona
        PaymentMethod paymentMethod = null;
        if (refundDto.getPaymentMethodNumber() != null) {
            paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(refundDto.getPaymentMethodNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Método de pago no encontrado con número: " + refundDto.getPaymentMethodNumber()));

            if (!paymentMethod.isActive()) {
                throw new PaymentProcessingException("El método de pago seleccionado no está activo");
            }
        } else if (originalPayment != null && originalPayment.getPaymentMethod() != null) {
            paymentMethod = originalPayment.getPaymentMethod();
        }

        // Crear entidad de reembolso
        Refund refund = mapper.toEntity(refundDto);
        refund.setRefundNumber(numberGenerator.generateRefundNumber());
        refund.setStatus(Refund.RefundStatus.REQUESTED);
        refund.setRequestDate(LocalDateTime.now());
        refund.setCreatedAt(LocalDateTime.now());

        if (paymentMethod != null) {
            refund.setPaymentMethod(paymentMethod);
        }

        Refund savedRefund = refundRepository.save(refund);

        // Publicar evento de solicitud de reembolso
        publishRefundRequestedEvent(savedRefund);

        // Notificar al cliente
        sendRefundNotification(savedRefund, "REQUESTED");

        return mapper.toDto(savedRefund);
    }

    @Override
    @Transactional
    public RefundDto processRefund(String refundNumber) {
        return lockService.executeWithLock("refund_process_" + refundNumber, () -> {
            Refund refund = refundRepository.findByRefundNumber(refundNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));

            // Validar estado
            if (refund.getStatus() != Refund.RefundStatus.APPROVED) {
                throw new PaymentProcessingException(
                        "El reembolso debe estar en estado APPROVED para ser procesado");
            }

            // Validar método de pago
            if (refund.getPaymentMethod() == null) {
                throw new PaymentProcessingException("Se requiere un método de pago para procesar el reembolso");
            }

            try {
                // Preparar metadatos
                Map<String, String> metadata = new HashMap<>();
                metadata.put("refundNumber", refund.getRefundNumber());
                if (refund.getPolicyNumber() != null) {
                    metadata.put("policyNumber", refund.getPolicyNumber());
                }
                if (refund.getOriginalPaymentNumber() != null) {
                    metadata.put("originalPaymentNumber", refund.getOriginalPaymentNumber());
                }

                // Procesar reembolso en la pasarela de pago
                TransactionDto transactionDto = paymentGatewayService.processRefund(
                        refund.getOriginalPaymentNumber(),
                        refund.getAmount(),
                        refund.getCurrency(),
                        refund.getReason(),
                        metadata
                );

                // Guardar transacción
                Transaction transaction = mapper.toEntity(transactionDto);
                transaction.setTransactionDate(LocalDateTime.now());
                transaction = transactionRepository.save(transaction);

                // Actualizar refund con la transacción
                refund.setTransaction(transaction);

                // Actualizar estado del reembolso
                if (transaction.getStatus() == Transaction.TransactionStatus.SUCCESSFUL) {
                    refund.setStatus(Refund.RefundStatus.COMPLETED);
                    refund.setProcessDate(LocalDateTime.now());

                    // Si hay pago original, actualizarlo a REFUNDED
                    if (refund.getOriginalPaymentNumber() != null) {
                        Payment originalPayment = paymentRepository.findByPaymentNumber(refund.getOriginalPaymentNumber())
                                .orElse(null);
                        if (originalPayment != null) {
                            originalPayment.setStatus(Payment.PaymentStatus.REFUNDED);
                            paymentRepository.save(originalPayment);
                        }
                    }
                } else if (transaction.getStatus() == Transaction.TransactionStatus.FAILED) {
                    refund.setStatus(Refund.RefundStatus.FAILED);
                }

                refund.setUpdatedAt(LocalDateTime.now());
                Refund savedRefund = refundRepository.save(refund);

                // Publicar evento de reembolso procesado
                publishRefundProcessedEvent(savedRefund, transaction);

                // Notificar al cliente
                sendRefundNotification(savedRefund, savedRefund.getStatus().name());

                return mapper.toDto(savedRefund);
            } catch (Exception e) {
                log.error("Error al procesar reembolso: {}", e.getMessage(), e);

                // Actualizar estado a fallido
                refund.setStatus(Refund.RefundStatus.FAILED);
                refund.setUpdatedAt(LocalDateTime.now());
                Refund savedRefund = refundRepository.save(refund);

                // Publicar evento de reembolso fallido
                publishRefundFailedEvent(savedRefund, e.getMessage());

                // Notificar al cliente
                sendRefundNotification(savedRefund, "FAILED");

                throw new PaymentProcessingException("Error al procesar reembolso: " + e.getMessage(), e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<RefundDto> processRefundAsync(String refundNumber) {
        return CompletableFuture.supplyAsync(() -> processRefund(refundNumber));
    }

    @Override
    public Optional<RefundDto> getRefundById(Long id) {
        return refundRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public Optional<RefundDto> getRefundByNumber(String refundNumber) {
        return refundRepository.findByRefundNumber(refundNumber).map(mapper::toDto);
    }

    @Override
    public List<RefundDto> getRefundsByCustomerNumber(String customerNumber) {
        return refundRepository.findByCustomerNumber(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundsByPolicyNumber(String policyNumber) {
        return refundRepository.findByPolicyNumber(policyNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundsByStatus(Refund.RefundStatus status) {
        return refundRepository.findByStatus(status).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<RefundDto> searchRefunds(String searchTerm, Pageable pageable) {
        return refundRepository.searchRefunds(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public RefundDto updateRefund(Long id, RefundDto refundDto) {
        return refundRepository.findById(id)
                .map(existingRefund -> {
                    // Actualizar solo campos editables
                    existingRefund.setReason(refundDto.getReason());
                    existingRefund.setDescription(refundDto.getDescription());
                    existingRefund.setUpdatedAt(LocalDateTime.now());

                    return mapper.toDto(refundRepository.save(existingRefund));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public RefundDto updateRefundStatus(String refundNumber, Refund.RefundStatus status, String reason) {
        return lockService.executeWithLock("refund_status_" + refundNumber, () -> {
            Refund refund = refundRepository.findByRefundNumber(refundNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));

            // Validar transición de estado
            validateStatusTransition(refund.getStatus(), status);

            // Actualizar estado
            refund.setStatus(status);
            if (reason != null) {
                refund.setDescription((refund.getDescription() != null ? refund.getDescription() + " | " : "") + reason);
            }
            refund.setUpdatedAt(LocalDateTime.now());

            Refund savedRefund = refundRepository.save(refund);

            // Notificar al cliente
            sendRefundNotification(savedRefund, status.name());

            return mapper.toDto(savedRefund);
        });
    }

    @Override
    @Transactional
    public RefundDto approveRefund(String refundNumber) {
        return updateRefundStatus(refundNumber, Refund.RefundStatus.APPROVED, "Reembolso aprobado");
    }

    @Override
    @Transactional
    public RefundDto rejectRefund(String refundNumber, String reason) {
        return updateRefundStatus(refundNumber, Refund.RefundStatus.REJECTED, reason);
    }

    @Override
    @Transactional
    public RefundDto completeRefund(String refundNumber, String externalReference) {
        return lockService.executeWithLock("refund_complete_" + refundNumber, () -> {
            Refund refund = refundRepository.findByRefundNumber(refundNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));

            // Validar estado
            if (refund.getStatus() != Refund.RefundStatus.APPROVED && refund.getStatus() != Refund.RefundStatus.PROCESSING) {
                throw new PaymentProcessingException(
                        "El reembolso debe estar en estado APPROVED o PROCESSING para ser completado manualmente");
            }

            // Crear transacción exitosa
            Transaction transaction = new Transaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setTransactionType(Transaction.TransactionType.REFUND);
            transaction.setAmount(refund.getAmount());
            transaction.setCurrency(refund.getCurrency());
            transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setGatewayReference(externalReference);
            transaction = transactionRepository.save(transaction);

            // Actualizar reembolso
            refund.setStatus(Refund.RefundStatus.COMPLETED);
            refund.setProcessDate(LocalDateTime.now());
            refund.setExternalReference(externalReference);
            refund.setTransaction(transaction);
            refund.setUpdatedAt(LocalDateTime.now());

            // Si hay pago original, actualizarlo a REFUNDED
            if (refund.getOriginalPaymentNumber() != null) {
                Payment originalPayment = paymentRepository.findByPaymentNumber(refund.getOriginalPaymentNumber())
                        .orElse(null);
                if (originalPayment != null) {
                    originalPayment.setStatus(Payment.PaymentStatus.REFUNDED);
                    paymentRepository.save(originalPayment);
                }
            }

            Refund savedRefund = refundRepository.save(refund);

            // Publicar evento de reembolso procesado
            publishRefundProcessedEvent(savedRefund, transaction);

            // Notificar al cliente
            sendRefundNotification(savedRefund, "COMPLETED");

            return mapper.toDto(savedRefund);
        });
    }

    @Override
    @Transactional
    public RefundDto cancelRefund(String refundNumber, String reason) {
        return lockService.executeWithLock("refund_cancel_" + refundNumber, () -> {
            Refund refund = refundRepository.findByRefundNumber(refundNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));

            // Validar estado
            if (refund.getStatus() != Refund.RefundStatus.REQUESTED && refund.getStatus() != Refund.RefundStatus.APPROVED) {
                throw new PaymentProcessingException(
                        "Solo se pueden cancelar reembolsos en estado REQUESTED o APPROVED");
            }

            // Actualizar estado
            refund.setStatus(Refund.RefundStatus.REJECTED);
            refund.setDescription((refund.getDescription() != null ? refund.getDescription() + " | " : "") +
                    "Cancelado: " + reason);
            refund.setUpdatedAt(LocalDateTime.now());

            Refund savedRefund = refundRepository.save(refund);

            // Notificar al cliente
            sendRefundNotification(savedRefund, "CANCELLED");

            return mapper.toDto(savedRefund);
        });
    }

    @Override
    public Optional<TransactionDto> getRefundTransaction(String refundNumber) {
        Refund refund = refundRepository.findByRefundNumber(refundNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));

        if (refund.getTransaction() == null) {
            return Optional.empty();
        }

        return Optional.of(mapper.toDto(refund.getTransaction()));
    }

    @Override
    public BigDecimal calculateTotalRefundedForCustomer(String customerNumber) {
        return refundRepository.sumCompletedRefundsByCustomer(customerNumber);
    }

    @Override
    public BigDecimal calculateTotalRefundedForPolicy(String policyNumber) {
        List<Refund> completedRefunds = refundRepository.findByPolicyNumber(policyNumber).stream()
                .filter(r -> r.getStatus() == Refund.RefundStatus.COMPLETED)
                .collect(Collectors.toList());

        return completedRefunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> getRefundStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> statistics = new HashMap<>();

        // Obtener reembolsos procesados en el período
        List<Refund> processedRefunds = refundRepository.findByProcessDateBetween(startDate, endDate);

        // Total de reembolsos procesados
        statistics.put("totalRefunds", processedRefunds.size());

        // Reembolsos por estado
        Map<Refund.RefundStatus, Long> refundsByStatus = processedRefunds.stream()
                .collect(Collectors.groupingBy(Refund::getStatus, Collectors.counting()));
        statistics.put("refundsByStatus", refundsByStatus);

        // Reembolsos por tipo
        Map<Refund.RefundType, Long> refundsByType = processedRefunds.stream()
                .collect(Collectors.groupingBy(Refund::getRefundType, Collectors.counting()));
        statistics.put("refundsByType", refundsByType);

        // Monto total reembolsado
        BigDecimal totalAmount = processedRefunds.stream()
                .filter(r -> r.getStatus() == Refund.RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalRefundedAmount", totalAmount);

        // Tiempo promedio de procesamiento (en horas)
        double avgProcessingTime = processedRefunds.stream()
                .filter(r -> r.getRequestDate() != null && r.getProcessDate() != null)
                .mapToDouble(r -> {
                    long hours = java.time.Duration.between(r.getRequestDate(), r.getProcessDate()).toHours();
                    return hours;
                })
                .average()
                .orElse(0);
        statistics.put("averageProcessingTimeHours", avgProcessingTime);

        return statistics;
    }

    @Override
    public byte[] generateRefundReport(LocalDateTime startDate, LocalDateTime endDate, String format) {
        // Obtener reembolsos en el rango de fechas
        List<Refund> refunds = refundRepository.findByProcessDateBetween(startDate, endDate);

        // Aquí se implementaría la lógica para generar el informe según el formato
        // Por ahora, solo devolvemos un texto simple
        String reportContent = "Informe de reembolsos del " + startDate + " al " + endDate + "\n";
        reportContent += "Total de reembolsos: " + refunds.size() + "\n";

        BigDecimal totalAmount = refunds.stream()
                .filter(r -> r.getStatus() == Refund.RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reportContent += "Monto total reembolsado: " + totalAmount + "\n\n";

        for (Refund refund : refunds) {
            reportContent += "Reembolso #" + refund.getRefundNumber() +
                    " - Cliente: " + refund.getCustomerNumber() +
                    " - Monto: " + refund.getAmount() +
                    " - Estado: " + refund.getStatus() +
                    " - Fecha: " + refund.getProcessDate() + "\n";
        }

        return reportContent.getBytes();
    }

    @Override
    @Async
    public CompletableFuture<Integer> processPendingRefunds() {
        List<Refund> approvedRefunds = refundRepository.findByStatus(Refund.RefundStatus.APPROVED);

        int processedCount = 0;
        for (Refund refund : approvedRefunds) {
            try {
                processRefund(refund.getRefundNumber());
                processedCount++;
            } catch (Exception e) {
                log.error("Error al procesar reembolso {}: {}", refund.getRefundNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(processedCount);
    }

    @Override
    @Async
    public CompletableFuture<Integer> notifyProcessedRefunds() {
        // Buscar reembolsos completados recientemente (últimas 24 horas)
        List<Refund> recentlyProcessed = refundRepository.findByProcessDateBetween(
                LocalDateTime.now().minusDays(1), LocalDateTime.now());

        int notifiedCount = 0;
        for (Refund refund : recentlyProcessed) {
            try {
                // Notificar al cliente
                sendRefundNotification(refund, "PROCESSED_CONFIRMATION");
                notifiedCount++;
            } catch (Exception e) {
                log.error("Error al notificar sobre reembolso {}: {}", refund.getRefundNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(notifiedCount);
    }

    // Métodos privados auxiliares

    private void validateStatusTransition(Refund.RefundStatus currentStatus, Refund.RefundStatus newStatus) {
        // Validar transiciones permitidas
        boolean isValid = false;

        switch (currentStatus) {
            case REQUESTED:
                isValid = (newStatus == Refund.RefundStatus.APPROVED ||
                        newStatus == Refund.RefundStatus.REJECTED);
                break;
            case APPROVED:
                isValid = (newStatus == Refund.RefundStatus.PROCESSING ||
                        newStatus == Refund.RefundStatus.COMPLETED ||
                        newStatus == Refund.RefundStatus.FAILED ||
                        newStatus == Refund.RefundStatus.REJECTED);
                break;
            case PROCESSING:
                isValid = (newStatus == Refund.RefundStatus.COMPLETED ||
                        newStatus == Refund.RefundStatus.FAILED);
                break;
            case COMPLETED:
                isValid = false; // Estado terminal
                break;
            case FAILED:
                isValid = (newStatus == Refund.RefundStatus.APPROVED); // Reintentar
                break;
            case REJECTED:
                isValid = false; // Estado terminal
                break;
        }

        if (!isValid) {
            throw new IllegalStateException(
                    "Transición de estado inválida: de " + currentStatus + " a " + newStatus);
        }
    }

    private void publishRefundRequestedEvent(Refund refund) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("refundNumber", refund.getRefundNumber());
        event.put("customerNumber", refund.getCustomerNumber());
        event.put("policyNumber", refund.getPolicyNumber());
        event.put("originalPaymentNumber", refund.getOriginalPaymentNumber());
        event.put("amount", refund.getAmount());
        event.put("currency", refund.getCurrency());
        event.put("reason", refund.getReason());
        event.put("requestDate", refund.getRequestDate());

        kafkaTemplate.send("refund-events", "requested", event);
    }

    private void publishRefundProcessedEvent(Refund refund, Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("refundNumber", refund.getRefundNumber());
        event.put("transactionId", transaction.getTransactionId());
        event.put("customerNumber", refund.getCustomerNumber());
        event.put("policyNumber", refund.getPolicyNumber());
        event.put("originalPaymentNumber", refund.getOriginalPaymentNumber());
        event.put("amount", refund.getAmount());
        event.put("currency", refund.getCurrency());
        event.put("status", refund.getStatus().name());
        event.put("successful", refund.getStatus() == Refund.RefundStatus.COMPLETED);
        event.put("gatewayReference", transaction.getGatewayReference());
        event.put("processDate", refund.getProcessDate());

        kafkaTemplate.send("refund-events", "processed", event);
    }

    private void publishRefundFailedEvent(Refund refund, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("refundNumber", refund.getRefundNumber());
        event.put("customerNumber", refund.getCustomerNumber());
        event.put("policyNumber", refund.getPolicyNumber());
        event.put("originalPaymentNumber", refund.getOriginalPaymentNumber());
        event.put("amount", refund.getAmount());
        event.put("currency", refund.getCurrency());
        event.put("reason", refund.getReason());
        event.put("errorMessage", errorMessage);
        event.put("failedAt", LocalDateTime.now());

        kafkaTemplate.send("refund-events", "failed", event);
    }

    private void sendRefundNotification(Refund refund, String eventType) {
        try {
            // Preparar notificación
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SYSTEM");
            notification.put("customerNumber", refund.getCustomerNumber());

            String title;
            String message;

            switch (eventType) {
                case "REQUESTED":
                    title = "Solicitud de reembolso recibida";
                    message = "Su solicitud de reembolso por " + refund.getAmount() + " " + refund.getCurrency() +
                            " ha sido recibida y está siendo procesada.";
                    break;
                case "APPROVED":
                    title = "Reembolso aprobado";
                    message = "Su solicitud de reembolso ha sido aprobada y será procesada en breve.";
                    break;
                case "COMPLETED":
                    title = "Reembolso completado";
                    message = "Su reembolso por " + refund.getAmount() + " " + refund.getCurrency() +
                            " ha sido procesado exitosamente.";
                    break;
                case "FAILED":
                    title = "Reembolso fallido";
                    message = "Hubo un problema al procesar su reembolso. Por favor, contacte con soporte.";
                    break;
                case "REJECTED":
                    title = "Reembolso rechazado";
                    message = "Su solicitud de reembolso ha sido rechazada. " +
                            (refund.getDescription() != null ? "Motivo: " + refund.getDescription() : "");
                    break;
                case "CANCELLED":
                    title = "Reembolso cancelado";
                    message = "Su solicitud de reembolso ha sido cancelada. " +
                            (refund.getDescription() != null ? "Motivo: " + refund.getDescription() : "");
                    break;
                case "PROCESSED_CONFIRMATION":
                    title = "Confirmación de reembolso";
                    message = "Confirmamos que su reembolso por " + refund.getAmount() + " " + refund.getCurrency() +
                            " ha sido procesado correctamente.";
                    break;
                default:
                    title = "Actualización de reembolso";
                    message = "Hay una actualización en su solicitud de reembolso.";
            }

            notification.put("title", title);
            notification.put("message", message);
            notification.put("refundNumber", refund.getRefundNumber());

            // Enviar notificación
            customerServiceClient.sendNotification("NOTIFICATION-" + UUID.randomUUID().toString(), notification);
        } catch (Exception e) {
            log.error("Error al enviar notificación de reembolso: {}", e.getMessage());
        }
    }
}