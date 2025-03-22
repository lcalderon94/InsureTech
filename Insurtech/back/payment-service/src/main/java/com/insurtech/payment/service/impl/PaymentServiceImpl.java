package com.insurtech.payment.service.impl;

import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.client.PolicyServiceClient;
import com.insurtech.payment.exception.InsufficientFundsException;
import com.insurtech.payment.exception.PaymentNotFoundException;
import com.insurtech.payment.exception.PaymentProcessingException;
import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.dto.PaymentRequestDto;
import com.insurtech.payment.model.dto.PaymentResponseDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Invoice;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.model.event.PaymentCreatedEvent;
import com.insurtech.payment.model.event.PaymentFailedEvent;
import com.insurtech.payment.model.event.PaymentProcessedEvent;
import com.insurtech.payment.repository.InvoiceRepository;
import com.insurtech.payment.repository.PaymentMethodRepository;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.repository.TransactionRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentGatewayService;
import com.insurtech.payment.service.PaymentService;
import com.insurtech.payment.util.EntityDtoMapper;
import com.insurtech.payment.util.PaymentNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final EntityDtoMapper mapper;
    private final PaymentNumberGenerator numberGenerator;
    private final PaymentGatewayService paymentGatewayService;
    private final DistributedLockService lockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PolicyServiceClient policyServiceClient;
    private final CustomerServiceClient customerServiceClient;

    @Override
    @Transactional
    public PaymentDto createPayment(PaymentDto paymentDto) {
        log.info("Creando nuevo pago para cliente número: {}", paymentDto.getCustomerNumber());

        // Validar existencia del cliente
        ResponseEntity<Boolean> customerExistsResponse = customerServiceClient.customerExists(paymentDto.getCustomerNumber());
        if (!customerExistsResponse.getBody()) {
            throw new ResourceNotFoundException("Cliente no encontrado con número: " + paymentDto.getCustomerNumber());
        }

        // Validar existencia de la póliza si se proporciona
        if (paymentDto.getPolicyNumber() != null && !paymentDto.getPolicyNumber().isEmpty()) {
            ResponseEntity<Boolean> policyExistsResponse = policyServiceClient.policyExists(paymentDto.getPolicyNumber());
            if (!policyExistsResponse.getBody()) {
                throw new ResourceNotFoundException("Póliza no encontrada con número: " + paymentDto.getPolicyNumber());
            }
        }

        // Validar método de pago si se proporciona
        PaymentMethod paymentMethod = null;
        if (paymentDto.getPaymentMethodNumber() != null) {
            paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentDto.getPaymentMethodNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Método de pago no encontrado con número: " + paymentDto.getPaymentMethodNumber()));

            if (!paymentMethod.isActive()) {
                throw new PaymentProcessingException("El método de pago seleccionado no está activo");
            }
        }

        // Validar factura si se proporciona
        Invoice invoice = null;
        if (paymentDto.getInvoiceNumber() != null) {
            invoice = invoiceRepository.findByInvoiceNumber(paymentDto.getInvoiceNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Factura no encontrada con número: " + paymentDto.getInvoiceNumber()));

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                throw new PaymentProcessingException("La factura ya ha sido pagada completamente");
            }
        }

        // Crear entidad de pago
        Payment payment = mapper.toEntity(paymentDto);
        payment.setPaymentNumber(numberGenerator.generatePaymentNumber());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        if (paymentMethod != null) {
            payment.setPaymentMethod(paymentMethod);
        }

        if (invoice != null) {
            payment.setInvoice(invoice);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Publicar evento de creación de pago
        publishPaymentCreatedEvent(savedPayment);

        return mapper.toDto(savedPayment);
    }

    @Override
    public PaymentDto processPayment(Long paymentId, PaymentMethodDto paymentMethodDto) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con ID: " + paymentId));

        // Convertir entidad a DTO
        PaymentDto paymentDto = mapper.toDto(payment);

        // Crear objeto de solicitud con los datos necesarios
        PaymentRequestDto requestDto = new PaymentRequestDto();
        requestDto.setCustomerNumber(payment.getCustomerNumber());
        requestDto.setPolicyNumber(payment.getPolicyNumber());
        requestDto.setAmount(payment.getAmount());
        requestDto.setCurrency(payment.getCurrency());
        requestDto.setConcept(payment.getConcept());
        requestDto.setDescription(payment.getDescription());
        requestDto.setPaymentMethodNumber(paymentMethodDto.getPaymentMethodNumber());

        // Procesar el pago
        PaymentResponseDto responseDto = processPayment(requestDto);

        // Si el procesamiento fue exitoso, actualizar el estado del pago original
        if (responseDto.isSuccessful()) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCompletionDate(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(responseDto.getErrorMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        }

        return mapper.toDto(payment);
    }

    /**
     * Procesa un pago específico con un método de pago dado
     * Este método es usado por el procesador asíncrono
     */
    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        log.info("Procesando pago para cliente número: {}", paymentRequestDto.getCustomerNumber());

        // Crear el pago
        PaymentDto paymentDto = createPaymentDtoFromRequest(paymentRequestDto);
        PaymentDto createdPayment = createPayment(paymentDto);

        // Procesar el pago
        String paymentNumber = createdPayment.getPaymentNumber();

        // Obtener método de pago
        PaymentMethod paymentMethod = null;
        PaymentMethodDto methodDto;

        if (paymentRequestDto.getPaymentMethodNumber() != null) {
            paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentRequestDto.getPaymentMethodNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Método de pago no encontrado con número: " + paymentRequestDto.getPaymentMethodNumber()));
            methodDto = mapper.toDto(paymentMethod);
        } else if (paymentRequestDto.getCardNumber() != null) {
            // Crear método de pago temporal para tarjeta
            methodDto = createTemporaryCardPaymentMethod(paymentRequestDto);
        } else {
            throw new PaymentProcessingException("Debe proporcionar un método de pago");
        }

        // Realizar transacción con la pasarela de pago
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("paymentNumber", paymentNumber);
            if (paymentRequestDto.getPolicyNumber() != null) {
                metadata.put("policyNumber", paymentRequestDto.getPolicyNumber());
            }
            if (paymentRequestDto.getInvoiceNumber() != null) {
                metadata.put("invoiceNumber", paymentRequestDto.getInvoiceNumber());
            }

            TransactionDto transactionDto = paymentGatewayService.processPaymentTransaction(
                    paymentRequestDto.getAmount(),
                    paymentRequestDto.getCurrency(),
                    methodDto,
                    paymentRequestDto.getConcept(),
                    metadata
            );

            // Guardar la transacción
            Transaction transaction = mapper.toEntity(transactionDto);
            Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                    .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));
            transaction.setPayment(payment);
            transaction = transactionRepository.save(transaction);

            // Actualizar estado del pago basado en el resultado de la transacción
            Payment.PaymentStatus newStatus = (transaction.getStatus() == Transaction.TransactionStatus.SUCCESSFUL) ?
                    Payment.PaymentStatus.COMPLETED : Payment.PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            payment.setPaymentDate(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Si es exitoso y hay factura, actualizar el estado de la factura
            if (newStatus == Payment.PaymentStatus.COMPLETED && payment.getInvoice() != null) {
                updateInvoiceAfterPayment(payment.getInvoice(), payment.getAmount());
            }

            // Publicar evento de pago procesado
            publishPaymentProcessedEvent(payment, transaction);

            // Guardar método de pago si el cliente lo solicitó
            if (paymentRequestDto.isSavePaymentMethod() && paymentMethod == null && transaction.getStatus() == Transaction.TransactionStatus.SUCCESSFUL) {
                saveNewPaymentMethod(paymentRequestDto);
            }

            // Crear respuesta
            return createPaymentResponse(payment, transaction);

        } catch (Exception e) {
            log.error("Error al procesar pago: {}", e.getMessage(), e);

            // Actualizar estado del pago a fallido
            Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                    .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // Crear transacción fallida
            Transaction failedTransaction = new Transaction();
            failedTransaction.setTransactionId(UUID.randomUUID().toString());
            failedTransaction.setTransactionType(Transaction.TransactionType.PAYMENT);
            failedTransaction.setAmount(paymentRequestDto.getAmount());
            failedTransaction.setCurrency(paymentRequestDto.getCurrency());
            failedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
            failedTransaction.setTransactionDate(LocalDateTime.now());
            failedTransaction.setErrorCode("PROCESSING_ERROR");
            failedTransaction.setErrorDescription(e.getMessage());
            failedTransaction.setPayment(payment);
            failedTransaction = transactionRepository.save(failedTransaction);

            // Publicar evento de pago fallido
            publishPaymentFailedEvent(payment, failedTransaction, e.getMessage());

            // Crear respuesta de error
            PaymentResponseDto errorResponse = new PaymentResponseDto();
            errorResponse.setPaymentNumber(paymentNumber);
            errorResponse.setSuccessful(false);
            errorResponse.setStatus("FAILED");
            errorResponse.setAmount(paymentRequestDto.getAmount());
            errorResponse.setCurrency(paymentRequestDto.getCurrency());
            errorResponse.setProcessingDate(LocalDateTime.now());
            errorResponse.setErrorMessage(e.getMessage());

            return errorResponse;
        }
    }


    @Override
    @Async
    public CompletableFuture<PaymentResponseDto> processPaymentAsync(PaymentRequestDto paymentRequestDto) {
        return CompletableFuture.supplyAsync(() -> processPayment(paymentRequestDto));
    }

    @Override
    public Optional<PaymentDto> getPaymentById(Long id) {
        return paymentRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public Optional<PaymentDto> getPaymentByNumber(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber).map(mapper::toDto);
    }

    @Override
    public List<PaymentDto> getPaymentsByCustomerNumber(String customerNumber) {
        return paymentRepository.findByCustomerNumber(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getPaymentsByPolicyNumber(String policyNumber) {
        return paymentRepository.findByPolicyNumber(policyNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentDto> searchPayments(String searchTerm, Pageable pageable) {
        return paymentRepository.searchPayments(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    public List<PaymentDto> getPaymentsByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDto updatePayment(Long id, PaymentDto paymentDto) {
        return paymentRepository.findById(id)
                .map(existingPayment -> {
                    // Actualizar solo campos editables
                    existingPayment.setConcept(paymentDto.getConcept());
                    existingPayment.setDescription(paymentDto.getDescription());
                    existingPayment.setReference(paymentDto.getReference());
                    existingPayment.setDueDate(paymentDto.getDueDate());
                    existingPayment.setUpdatedAt(LocalDateTime.now());

                    return mapper.toDto(paymentRepository.save(existingPayment));
                })
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public PaymentDto updatePaymentStatus(String paymentNumber, Payment.PaymentStatus status, String reason) {
        return lockService.executeWithLock("payment_status_" + paymentNumber, () -> {
            Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                    .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));

            // Validar transición de estado
            validateStatusTransition(payment.getStatus(), status);

            // Actualizar estado
            payment.setStatus(status);
            payment.setUpdatedAt(LocalDateTime.now());

            // Si el pago se completa, actualizar fecha de pago
            if (status == Payment.PaymentStatus.COMPLETED && payment.getPaymentDate() == null) {
                payment.setPaymentDate(LocalDateTime.now());

                // Si hay factura, actualizar su estado
                if (payment.getInvoice() != null) {
                    updateInvoiceAfterPayment(payment.getInvoice(), payment.getAmount());
                }
            }

            // Guardar transacción de cambio de estado
            Transaction statusTransaction = new Transaction();
            statusTransaction.setTransactionId(UUID.randomUUID().toString());
            statusTransaction.setTransactionType(Transaction.TransactionType.ADJUSTMENT);
            statusTransaction.setAmount(payment.getAmount());
            statusTransaction.setCurrency(payment.getCurrency());
            statusTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
            statusTransaction.setTransactionDate(LocalDateTime.now());
            statusTransaction.setGatewayResponseMessage(reason);
            statusTransaction.setPayment(payment);
            transactionRepository.save(statusTransaction);

            return mapper.toDto(paymentRepository.save(payment));
        });
    }

    @Override
    @Transactional
    public PaymentDto cancelPayment(String paymentNumber, String reason) {
        return lockService.executeWithLock("payment_cancel_" + paymentNumber, () -> {
            Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                    .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));

            // Solo se pueden cancelar pagos pendientes
            if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
                throw new PaymentProcessingException("Solo se pueden cancelar pagos en estado PENDING");
            }

            // Actualizar estado
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());

            // Guardar transacción de cancelación
            Transaction cancelTransaction = new Transaction();
            cancelTransaction.setTransactionId(UUID.randomUUID().toString());
            cancelTransaction.setTransactionType(Transaction.TransactionType.VOID);
            cancelTransaction.setAmount(payment.getAmount());
            cancelTransaction.setCurrency(payment.getCurrency());
            cancelTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
            cancelTransaction.setTransactionDate(LocalDateTime.now());
            cancelTransaction.setGatewayResponseMessage(reason);
            cancelTransaction.setPayment(payment);
            transactionRepository.save(cancelTransaction);

            Payment savedPayment = paymentRepository.save(payment);

            // Publicar evento de pago cancelado
            publishPaymentCancelledEvent(savedPayment, reason);

            return mapper.toDto(savedPayment);
        });
    }

    @Override
    public List<TransactionDto> getTransactionsByPaymentNumber(String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));

        return transactionRepository.findByPaymentId(payment.getId()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionDto createTransaction(String paymentNumber, TransactionDto transactionDto) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));

        Transaction transaction = mapper.toEntity(transactionDto);
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPayment(payment);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Actualizar estado del pago según resultado de la transacción
        updatePaymentStatusBasedOnTransaction(payment, savedTransaction);

        return mapper.toDto(savedTransaction);
    }

    @Override
    public BigDecimal calculateTotalPaidForPolicy(String policyNumber) {
        List<Payment> completedPayments = paymentRepository.findByPolicyNumber(policyNumber).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        return completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean isPaymentCompleted(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber)
                .map(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .orElse(false);
    }

    @Override
    public Map<String, Object> getPaymentStatistics(String customerNumber) {
        Map<String, Object> statistics = new HashMap<>();

        List<Payment> customerPayments = paymentRepository.findByCustomerNumber(customerNumber);

        // Total de pagos
        statistics.put("totalPayments", customerPayments.size());

        // Pagos completados
        long completedCount = customerPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .count();
        statistics.put("completedPayments", completedCount);

        // Pagos pendientes
        long pendingCount = customerPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .count();
        statistics.put("pendingPayments", pendingCount);

        // Pagos fallidos
        long failedCount = customerPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.FAILED)
                .count();
        statistics.put("failedPayments", failedCount);

        // Monto total pagado
        BigDecimal totalPaid = customerPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalAmountPaid", totalPaid);

        // Monto pendiente
        BigDecimal pendingAmount = customerPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("pendingAmount", pendingAmount);

        return statistics;
    }

    @Override
    public Map<String, Object> getPaymentStatisticsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> statistics = new HashMap<>();

        // Obtener pagos completados en el período
        List<Payment> completedPayments = paymentRepository.findCompletedPaymentsWithinDateRange(startDate, endDate);

        // Total de pagos completados
        statistics.put("totalPayments", completedPayments.size());

        // Monto total pagado
        BigDecimal totalAmount = completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalAmount", totalAmount);

        // Pagos por tipo
        Map<Payment.PaymentType, Long> paymentsByType = completedPayments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentType, Collectors.counting()));
        statistics.put("paymentsByType", paymentsByType);

        // Montos por tipo
        Map<Payment.PaymentType, BigDecimal> amountsByType = completedPayments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentType,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
        statistics.put("amountsByType", amountsByType);

        // Montos por día
        Map<LocalDateTime, BigDecimal> amountsByDay = completedPayments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentDate().toLocalDate().atStartOfDay(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
        statistics.put("amountsByDay", amountsByDay);

        return statistics;
    }

    @Override
    public List<PaymentDto> getPendingPaymentsDueSoon(int daysAhead) {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(daysAhead);

        return paymentRepository.findPendingPaymentsWithDueDateBetween(startDate, endDate).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Async
    public CompletableFuture<Integer> reconcilePayments(List<String> externalReferences) {
        int updatedCount = 0;

        for (String externalRef : externalReferences) {
            try {
                // Buscar transacciones pendientes de reconciliación
                List<Transaction> transactions = transactionRepository.findTransactionsForReconciliation(LocalDateTime.now().minusDays(30));

                for (Transaction tx : transactions) {
                    if (externalRef.equals(tx.getGatewayReference())) {
                        // Verificar estado actual con la pasarela
                        Transaction.TransactionStatus gatewayStatus = paymentGatewayService.checkTransactionStatus(tx.getTransactionId());

                        if (gatewayStatus != tx.getStatus()) {
                            // Actualizar estado
                            tx.setStatus(gatewayStatus);
                            tx.setReconciled(true);
                            tx.setReconciliationDate(LocalDateTime.now());
                            transactionRepository.save(tx);

                            // Actualizar pago si es necesario
                            updatePaymentStatusBasedOnTransaction(tx.getPayment(), tx);

                            updatedCount++;
                        } else {
                            // Marcar como reconciliada
                            tx.setReconciled(true);
                            tx.setReconciliationDate(LocalDateTime.now());
                            transactionRepository.save(tx);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error al reconciliar pago con referencia: {}", externalRef, e);
            }
        }

        return CompletableFuture.completedFuture(updatedCount);
    }

    @Override
    public byte[] generatePaymentReport(LocalDateTime startDate, LocalDateTime endDate, String format) {
        // Obtener pagos en el rango de fechas
        List<Payment> payments = paymentRepository.findCompletedPaymentsWithinDateRange(startDate, endDate);

        // Aquí se implementaría la lógica para generar el informe según el formato
        // Por ahora, solo devolvemos un texto simple
        String reportContent = "Informe de pagos del " + startDate + " al " + endDate + "\n";
        reportContent += "Total de pagos: " + payments.size() + "\n";

        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reportContent += "Monto total: " + totalAmount + "\n\n";

        for (Payment payment : payments) {
            reportContent += "Pago #" + payment.getPaymentNumber() +
                    " - Cliente: " + payment.getCustomerNumber() +
                    " - Monto: " + payment.getAmount() +
                    " - Fecha: " + payment.getPaymentDate() + "\n";
        }

        return reportContent.getBytes();
    }

    @Override
    @Async
    public CompletableFuture<Integer> updatePaymentsInBatch(List<String> paymentNumbers, Payment.PaymentStatus status) {
        int updatedCount = 0;

        for (String number : paymentNumbers) {
            try {
                updatePaymentStatus(number, status, "Actualización masiva");
                updatedCount++;
            } catch (Exception e) {
                log.error("Error al actualizar pago {}: {}", number, e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(updatedCount);
    }

    // Métodos privados auxiliares

    private PaymentDto createPaymentDtoFromRequest(PaymentRequestDto request) {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setCustomerNumber(request.getCustomerNumber());
        paymentDto.setPolicyNumber(request.getPolicyNumber());
        paymentDto.setInvoiceNumber(request.getInvoiceNumber());
        paymentDto.setAmount(request.getAmount());
        paymentDto.setCurrency(request.getCurrency());
        paymentDto.setConcept(request.getConcept());
        paymentDto.setDescription(request.getDescription());
        paymentDto.setPaymentMethodNumber(request.getPaymentMethodNumber());
        paymentDto.setPaymentType(Payment.PaymentType.PREMIUM); // Por defecto

        return paymentDto;
    }

    private PaymentMethodDto createTemporaryCardPaymentMethod(PaymentRequestDto request) {
        PaymentMethodDto methodDto = new PaymentMethodDto();
        methodDto.setCustomerNumber(request.getCustomerNumber());
        methodDto.setMethodType(PaymentMethod.MethodType.CREDIT_CARD);
        methodDto.setName("Tarjeta de pago único");
        methodDto.setCardHolderName(request.getCardHolderName());
        methodDto.setFullCardNumber(request.getCardNumber());

        // Crear fecha de expiración
        int month = Integer.parseInt(request.getCardExpiryMonth());
        int year = Integer.parseInt(request.getCardExpiryYear());
        methodDto.setCardExpiryDate(java.time.YearMonth.of(year, month));

        methodDto.setCvv(request.getCvv());
        methodDto.setActive(true);

        return methodDto;
    }

    private void updateInvoiceAfterPayment(Invoice invoice, BigDecimal paymentAmount) {
        // Obtener la factura actualizada de la base de datos
        Invoice currentInvoice = invoiceRepository.findById(invoice.getId()).orElse(invoice);

        // Calcular pago acumulado
        BigDecimal currentPaidAmount = currentInvoice.getPaidAmount() != null ?
                currentInvoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaidAmount.add(paymentAmount);
        currentInvoice.setPaidAmount(newPaidAmount);

        // Actualizar estado basado en el monto pagado
        if (newPaidAmount.compareTo(currentInvoice.getTotalAmount()) >= 0) {
            currentInvoice.setStatus(Invoice.InvoiceStatus.PAID);
            currentInvoice.setPaymentDate(LocalDateTime.now());
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            currentInvoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(currentInvoice);
    }

    private void validateStatusTransition(Payment.PaymentStatus currentStatus, Payment.PaymentStatus newStatus) {
        // Validar transiciones permitidas
        boolean isValid = false;

        switch (currentStatus) {
            case PENDING:
                isValid = (newStatus == Payment.PaymentStatus.PROCESSING ||
                        newStatus == Payment.PaymentStatus.COMPLETED ||
                        newStatus == Payment.PaymentStatus.FAILED ||
                        newStatus == Payment.PaymentStatus.CANCELLED);
                break;
            case PROCESSING:
                isValid = (newStatus == Payment.PaymentStatus.COMPLETED ||
                        newStatus == Payment.PaymentStatus.FAILED);
                break;
            case FAILED:
                isValid = (newStatus == Payment.PaymentStatus.PENDING ||
                        newStatus == Payment.PaymentStatus.CANCELLED);
                break;
            case CANCELLED:
                isValid = false; // Estado terminal
                break;
            case COMPLETED:
                isValid = (newStatus == Payment.PaymentStatus.REFUNDED);
                break;
            case REFUNDED:
                isValid = false; // Estado terminal
                break;
        }

        if (!isValid) {
            throw new PaymentProcessingException(
                    "Transición de estado inválida: de " + currentStatus + " a " + newStatus);
        }
    }

    private void updatePaymentStatusBasedOnTransaction(Payment payment, Transaction transaction) {
        if (transaction.getStatus() == Transaction.TransactionStatus.SUCCESSFUL) {
            if (transaction.getTransactionType() == Transaction.TransactionType.PAYMENT) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());

                // Si hay factura, actualizar su estado
                if (payment.getInvoice() != null) {
                    updateInvoiceAfterPayment(payment.getInvoice(), payment.getAmount());
                }
            } else if (transaction.getTransactionType() == Transaction.TransactionType.VOID) {
                payment.setStatus(Payment.PaymentStatus.CANCELLED);
            } else if (transaction.getTransactionType() == Transaction.TransactionType.REFUND) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
            }
        } else if (transaction.getStatus() == Transaction.TransactionStatus.FAILED) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        } else if (transaction.getStatus() == Transaction.TransactionStatus.PROCESSING ||
                transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private PaymentResponseDto createPaymentResponse(Payment payment, Transaction transaction) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentNumber(payment.getPaymentNumber());
        response.setTransactionId(transaction.getTransactionId());
        response.setStatus(payment.getStatus().name());
        response.setSuccessful(payment.getStatus() == Payment.PaymentStatus.COMPLETED);
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setProcessingDate(transaction.getTransactionDate());
        response.setAuthorizationCode(transaction.getAuthorizationCode());

        // Información de tarjeta si aplica
        if (payment.getPaymentMethod() != null &&
                (payment.getPaymentMethod().getMethodType() == PaymentMethod.MethodType.CREDIT_CARD ||
                        payment.getPaymentMethod().getMethodType() == PaymentMethod.MethodType.DEBIT_CARD)) {
            response.setPaymentMethodNumber(payment.getPaymentMethod().getPaymentMethodNumber());
            response.setMaskedCardNumber(payment.getPaymentMethod().getMaskedCardNumber());
            response.setCardType(payment.getPaymentMethod().getCardType());
        }

        // Si hubo error
        if (transaction.getStatus() == Transaction.TransactionStatus.FAILED) {
            response.setErrorCode(transaction.getErrorCode());
            response.setErrorMessage(transaction.getErrorDescription());
        }

        return response;
    }

    private PaymentMethodDto saveNewPaymentMethod(PaymentRequestDto request) {
        // Crear DTO del método de pago
        PaymentMethodDto methodDto = new PaymentMethodDto();
        methodDto.setCustomerNumber(request.getCustomerNumber());
        methodDto.setMethodType(PaymentMethod.MethodType.CREDIT_CARD);
        methodDto.setName(request.getCardHolderName() + " - " + request.getCardNumber().substring(request.getCardNumber().length() - 4));
        methodDto.setActive(true);
        methodDto.setCardHolderName(request.getCardHolderName());
        methodDto.setFullCardNumber(request.getCardNumber());

        // Crear fecha de expiración
        int month = Integer.parseInt(request.getCardExpiryMonth());
        int year = Integer.parseInt(request.getCardExpiryYear());
        methodDto.setCardExpiryDate(java.time.YearMonth.of(year, month));

        methodDto.setCvv(request.getCvv());

        // Tokenizar tarjeta para almacenamiento seguro
        String token = paymentGatewayService.tokenizePaymentMethod(methodDto);

        // Crear entidad
        PaymentMethod paymentMethod = mapper.toEntity(methodDto);
        paymentMethod.setPaymentMethodNumber(numberGenerator.generatePaymentMethodNumber());

        // Enmascarar número de tarjeta
        String maskedNumber = "XXXX-XXXX-XXXX-" + request.getCardNumber().substring(request.getCardNumber().length() - 4);
        paymentMethod.setMaskedCardNumber(maskedNumber);

        // Guardar token en lugar del número real
        paymentMethod.setPaymentToken(token);
        paymentMethod.setTokenExpiryDate(LocalDateTime.now().plusYears(1));

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        return mapper.toDto(savedMethod);
    }

    private void publishPaymentCreatedEvent(Payment payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setPaymentNumber(payment.getPaymentNumber());
        event.setCustomerNumber(payment.getCustomerNumber());
        event.setPolicyNumber(payment.getPolicyNumber());
        event.setPaymentType(payment.getPaymentType());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setStatus(payment.getStatus());
        event.setCreatedAt(payment.getCreatedAt());

        if (payment.getPaymentMethod() != null) {
            event.setPaymentMethodNumber(payment.getPaymentMethod().getPaymentMethodNumber());
        }

        kafkaTemplate.send("payment-events", "created", event);
    }

    private void publishPaymentProcessedEvent(Payment payment, Transaction transaction) {
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setPaymentNumber(payment.getPaymentNumber());
        event.setTransactionId(transaction.getTransactionId());
        event.setCustomerNumber(payment.getCustomerNumber());
        event.setPolicyNumber(payment.getPolicyNumber());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setSuccessful(transaction.getStatus() == Transaction.TransactionStatus.SUCCESSFUL);
        event.setStatus(payment.getStatus());
        event.setGatewayReference(transaction.getGatewayReference());
        event.setAuthorizationCode(transaction.getAuthorizationCode());
        event.setErrorCode(transaction.getErrorCode());
        event.setErrorMessage(transaction.getErrorDescription());
        event.setProcessedAt(transaction.getTransactionDate());

        kafkaTemplate.send("payment-events", "processed", event);
    }

    private void publishPaymentFailedEvent(Payment payment, Transaction transaction, String failureReason) {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setPaymentNumber(payment.getPaymentNumber());
        event.setTransactionId(transaction.getTransactionId());
        event.setCustomerNumber(payment.getCustomerNumber());
        event.setPolicyNumber(payment.getPolicyNumber());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setFailureReason(failureReason);
        event.setErrorCode(transaction.getErrorCode());
        event.setErrorMessage(transaction.getErrorDescription());
        event.setRetryable(transaction.getRetryCount() < 3);
        event.setRetryCount(transaction.getRetryCount());
        event.setRetryScheduledAt(transaction.getRetryDate());
        event.setFailedAt(transaction.getTransactionDate());

        kafkaTemplate.send("payment-events", "failed", event);
    }

    private void publishPaymentCancelledEvent(Payment payment, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("paymentNumber", payment.getPaymentNumber());
        event.put("customerNumber", payment.getCustomerNumber());
        event.put("policyNumber", payment.getPolicyNumber());
        event.put("amount", payment.getAmount());
        event.put("currency", payment.getCurrency());
        event.put("reason", reason);
        event.put("cancelledAt", LocalDateTime.now());

        kafkaTemplate.send("payment-events", "cancelled", event);
    }


}