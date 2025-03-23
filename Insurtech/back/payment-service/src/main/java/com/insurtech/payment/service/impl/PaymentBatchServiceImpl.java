package com.insurtech.payment.service.impl;

import com.insurtech.payment.exception.PaymentNotFoundException;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentRequestDto;
import com.insurtech.payment.model.dto.PaymentResponseDto;
import com.insurtech.payment.model.entity.Invoice;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.repository.InvoiceRepository;
import com.insurtech.payment.repository.PaymentMethodRepository;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.repository.TransactionRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentBatchService;
import com.insurtech.payment.service.PaymentGatewayService;
import com.insurtech.payment.service.PaymentService;
import com.insurtech.payment.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentBatchServiceImpl implements PaymentBatchService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
    private final PaymentGatewayService paymentGatewayService;
    private final DistributedLockService lockService;
    private final EntityDtoMapper mapper;
    private final PaymentMethodRepository paymentMethodRepository;


    // Mapa para seguimiento del estado de los trabajos por lotes
    private final Map<String, Map<String, Object>> batchStatus = new ConcurrentHashMap<>();

    @Override
    @Async
    public CompletableFuture<List<PaymentDto>> processBatch(List<PaymentDto> payments) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("totalItems", payments.size());
        status.put("processedItems", 0);
        status.put("successCount", 0);
        status.put("failureCount", 0);
        status.put("status", "PROCESSING");
        status.put("errors", new ArrayList<String>());
        batchStatus.put(batchId, status);

        List<PaymentDto> processedPayments = new ArrayList<>();

        try {
            for (PaymentDto payment : payments) {
                try {
                    // Crear pago
                    PaymentDto processedPayment = paymentService.createPayment(payment);
                    processedPayments.add(processedPayment);

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("successCount", (int) status.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al procesar pago en lote: {}", e.getMessage());

                    // Registrar error
                    List<String> errors = (List<String>) status.get("errors");
                    errors.add("Error al procesar pago para cliente " + payment.getCustomerNumber() + ": " + e.getMessage());

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("failureCount", (int) status.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            status.put("endTime", LocalDateTime.now());
            status.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(processedPayments);
        } catch (Exception e) {
            log.error("Error general en procesamiento por lotes: {}", e.getMessage());

            // Actualizar estado final con error
            status.put("endTime", LocalDateTime.now());
            status.put("status", "FAILED");
            status.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<Map<String, Object>> analyzePaymentMethodEffectiveness() {
        Map<String, Object> analysis = new HashMap<>();

        try {
            // Obtener todos los pagos con método de pago asociado
            List<Payment> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getPaymentMethod() != null)
                    .collect(Collectors.toList());

            // Agrupar por tipo de método de pago
            Map<PaymentMethod.MethodType, List<Payment>> paymentsByMethodType = payments.stream()
                    .collect(Collectors.groupingBy(p -> p.getPaymentMethod().getMethodType()));

            // Calcular estadísticas para cada tipo
            Map<String, Map<String, Object>> effectivenessByType = new HashMap<>();

            for (Map.Entry<PaymentMethod.MethodType, List<Payment>> entry : paymentsByMethodType.entrySet()) {
                PaymentMethod.MethodType methodType = entry.getKey();
                List<Payment> methodPayments = entry.getValue();

                Map<String, Object> stats = new HashMap<>();

                // Total de pagos
                stats.put("totalPayments", methodPayments.size());

                // Pagos exitosos
                long successfulPayments = methodPayments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .count();

                // Tasa de éxito
                double successRate = methodPayments.isEmpty() ? 0 : (double) successfulPayments / methodPayments.size() * 100;
                stats.put("successRate", Math.round(successRate * 100) / 100.0);

                // Tiempo promedio de procesamiento
                OptionalDouble avgProcessingTime = methodPayments.stream()
                        .filter(p -> p.getPaymentDate() != null && p.getCreatedAt() != null)
                        .mapToLong(p -> ChronoUnit.SECONDS.between(p.getCreatedAt(), p.getPaymentDate()))
                        .average();

                stats.put("avgProcessingTimeSeconds", avgProcessingTime.isPresent() ? avgProcessingTime.getAsDouble() : 0);

                // Monto promedio de pago
                OptionalDouble avgAmount = methodPayments.stream()
                        .mapToDouble(p -> p.getAmount().doubleValue())
                        .average();

                stats.put("avgAmount", avgAmount.isPresent() ? avgAmount.getAsDouble() : 0);

                effectivenessByType.put(methodType.toString(), stats);
            }

            analysis.put("effectivenessByMethodType", effectivenessByType);

            // Método de pago más efectivo (mayor tasa de éxito)
            Map.Entry<String, Map<String, Object>> mostEffective = effectivenessByType.entrySet().stream()
                    .max(Comparator.comparing(e -> ((Number) e.getValue().get("successRate")).doubleValue()))
                    .orElse(null);

            if (mostEffective != null) {
                analysis.put("mostEffectiveMethod", mostEffective.getKey());
                analysis.put("mostEffectiveMethodStats", mostEffective.getValue());
            }

            return CompletableFuture.completedFuture(analysis);

        } catch (Exception e) {
            log.error("Error al analizar efectividad de métodos de pago: {}", e.getMessage());
            analysis.put("error", e.getMessage());
            return CompletableFuture.completedFuture(analysis);
        }
    }

    public Map<String, Object> configureScheduledPayments(Map<String, Object> schedule) {
        // Implementación de la configuración de pagos programados
        // En un entorno real, esto podría configurar tareas programadas en el sistema
        Map<String, Object> result = new HashMap<>();

        try {
            // Ejemplo de horario: días de la semana, hora del día
            List<String> days = (List<String>) schedule.get("days");
            String time = (String) schedule.get("time");
            boolean notifyCustomers = Boolean.parseBoolean(schedule.getOrDefault("notifyCustomers", "false").toString());

            // Guardar configuración (en un entorno real, esto iría a una BD o archivo de configuración)
            result.put("configured", true);
            result.put("days", days);
            result.put("time", time);
            result.put("notifyCustomers", notifyCustomers);
            result.put("message", "Configuración de pagos programados establecida correctamente");

        } catch (Exception e) {
            log.error("Error al configurar pagos programados: {}", e.getMessage());
            result.put("configured", false);
            result.put("error", e.getMessage());
        }

        return result;
    }



    /**
     * Procesa un lote de pagos pendientes con un método de pago específico
     */
    @Async
    public CompletableFuture<List<PaymentDto>> processPendingPaymentsBatch(PaymentMethodDto paymentMethodDto) {
        log.info("Procesando lote de pagos pendientes con método de pago: {}", paymentMethodDto.getPaymentMethodNumber());

        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("operation", "PENDING_PAYMENTS");
        batchStatus.put(batchId, status);

        try {
            // Obtener pagos pendientes para el cliente
            List<Payment> pendingPayments = paymentRepository.findByCustomerNumberAndStatus(
                    paymentMethodDto.getCustomerNumber(), Payment.PaymentStatus.PENDING);

            status.put("totalItems", pendingPayments.size());
            status.put("processedItems", 0);
            status.put("successCount", 0);
            status.put("failureCount", 0);

            List<PaymentDto> processedPayments = new ArrayList<>();

            for (Payment payment : pendingPayments) {
                try {
                    PaymentDto paymentDto = mapper.toDto(payment);
                    PaymentRequestDto requestDto = new PaymentRequestDto();
                    requestDto.setCustomerNumber(payment.getCustomerNumber());
                    requestDto.setPolicyNumber(payment.getPolicyNumber());
                    requestDto.setAmount(payment.getAmount());
                    requestDto.setCurrency(payment.getCurrency());
                    requestDto.setConcept(payment.getConcept());
                    requestDto.setDescription(payment.getDescription());
                    requestDto.setPaymentMethodNumber(paymentMethodDto.getPaymentMethodNumber());

                    PaymentResponseDto response = paymentService.processPayment(requestDto);
                    PaymentDto processed = paymentService.getPaymentByNumber(payment.getPaymentNumber())
                            .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado: " + payment.getPaymentNumber()));

                    processedPayments.add(processed);

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("successCount", (int) status.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al procesar pago ID {} en lote: {}", payment.getId(), e.getMessage());

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("failureCount", (int) status.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            status.put("endTime", LocalDateTime.now());
            status.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(processedPayments);
        } catch (Exception e) {
            log.error("Error general en procesamiento de pagos pendientes: {}", e.getMessage());

            // Actualizar estado final con error
            status.put("endTime", LocalDateTime.now());
            status.put("status", "FAILED");
            status.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<List<PaymentDto>> processPaymentsFromCsv(InputStream inputStream) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("source", "CSV");
        status.put("processedItems", 0);
        status.put("successCount", 0);
        status.put("failureCount", 0);
        status.put("errors", new ArrayList<String>());
        batchStatus.put(batchId, status);

        List<PaymentDto> processedPayments = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<CSVRecord> records = csvParser.getRecords();
            status.put("totalItems", records.size());

            for (CSVRecord record : records) {
                try {
                    // Crear DTO desde el registro CSV
                    PaymentDto paymentDto = new PaymentDto();
                    paymentDto.setCustomerNumber(record.get("customerNumber"));
                    paymentDto.setPolicyNumber(record.get("policyNumber"));
                    if (record.isMapped("invoiceNumber")) {
                        paymentDto.setInvoiceNumber(record.get("invoiceNumber"));
                    }
                    paymentDto.setAmount(new BigDecimal(record.get("amount")));
                    paymentDto.setCurrency(record.get("currency"));
                    paymentDto.setConcept(record.get("concept"));
                    if (record.isMapped("description")) {
                        paymentDto.setDescription(record.get("description"));
                    }
                    if (record.isMapped("paymentMethodNumber")) {
                        paymentDto.setPaymentMethodNumber(record.get("paymentMethodNumber"));
                    }
                    if (record.isMapped("paymentType")) {
                        paymentDto.setPaymentType(Payment.PaymentType.valueOf(record.get("paymentType")));
                    } else {
                        paymentDto.setPaymentType(Payment.PaymentType.PREMIUM);
                    }
                    if (record.isMapped("dueDate")) {
                        paymentDto.setDueDate(LocalDateTime.parse(record.get("dueDate")));
                    }

                    // Crear pago
                    PaymentDto processedPayment = paymentService.createPayment(paymentDto);
                    processedPayments.add(processedPayment);

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("successCount", (int) status.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al procesar registro CSV para pago: {}", e.getMessage());

                    // Registrar error
                    List<String> errors = (List<String>) status.get("errors");
                    errors.add("Error en línea " + record.getRecordNumber() + ": " + e.getMessage());

                    // Actualizar estado
                    status.put("processedItems", (int) status.get("processedItems") + 1);
                    status.put("failureCount", (int) status.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            status.put("endTime", LocalDateTime.now());
            status.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(processedPayments);
        } catch (Exception e) {
            log.error("Error general en procesamiento de CSV: {}", e.getMessage());

            // Actualizar estado final con error
            status.put("endTime", LocalDateTime.now());
            status.put("status", "FAILED");
            status.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> batchStatusUpdate(List<String> paymentNumbers, Payment.PaymentStatus status, String reason) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("totalItems", paymentNumbers.size());
        jobStatus.put("processedItems", 0);
        jobStatus.put("successCount", 0);
        jobStatus.put("failureCount", 0);
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "STATUS_UPDATE");
        jobStatus.put("errors", new ArrayList<String>());
        batchStatus.put(batchId, jobStatus);

        int updatedCount = 0;

        try {
            for (String paymentNumber : paymentNumbers) {
                try {
                    paymentService.updatePaymentStatus(paymentNumber, status, reason);
                    updatedCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al actualizar estado del pago {}: {}", paymentNumber, e.getMessage());

                    // Registrar error
                    List<String> errors = (List<String>) jobStatus.get("errors");
                    errors.add("Error al actualizar pago " + paymentNumber + ": " + e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(updatedCount);
        } catch (Exception e) {
            log.error("Error general en actualización masiva de estados: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> batchCancelPayments(List<String> paymentNumbers, String reason) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("totalItems", paymentNumbers.size());
        jobStatus.put("processedItems", 0);
        jobStatus.put("successCount", 0);
        jobStatus.put("failureCount", 0);
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "CANCEL_PAYMENTS");
        jobStatus.put("errors", new ArrayList<String>());
        batchStatus.put(batchId, jobStatus);

        int cancelledCount = 0;

        try {
            for (String paymentNumber : paymentNumbers) {
                try {
                    paymentService.cancelPayment(paymentNumber, reason);
                    cancelledCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al cancelar pago {}: {}", paymentNumber, e.getMessage());

                    // Registrar error
                    List<String> errors = (List<String>) jobStatus.get("errors");
                    errors.add("Error al cancelar pago " + paymentNumber + ": " + e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(cancelledCount);
        } catch (Exception e) {
            log.error("Error general en cancelación masiva de pagos: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<byte[]> exportPayments(List<String> paymentNumbers, List<Payment.PaymentStatus> statuses,
                                                    LocalDateTime startDate, LocalDateTime endDate, String format) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "EXPORT_PAYMENTS");
        jobStatus.put("format", format);
        batchStatus.put(batchId, jobStatus);

        try {
            // Preparar filtros
            List<Payment> paymentsToExport = new ArrayList<>();

            if (paymentNumbers != null && !paymentNumbers.isEmpty()) {
                // Filtrar por números de pago
                for (String number : paymentNumbers) {
                    paymentRepository.findByPaymentNumber(number).ifPresent(paymentsToExport::add);
                }
            } else if (statuses != null && !statuses.isEmpty()) {
                // Filtrar por estados
                for (Payment.PaymentStatus status : statuses) {
                    paymentsToExport.addAll(paymentRepository.findByStatus(status));
                }
            } else if (startDate != null && endDate != null) {
                // Filtrar por rango de fechas
                paymentsToExport.addAll(paymentRepository.findCompletedPaymentsWithinDateRange(startDate, endDate));
            } else {
                // Sin filtros, limitar a los últimos 1000 pagos
                paymentsToExport = paymentRepository.findAll().stream()
                        .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                        .limit(1000)
                        .collect(Collectors.toList());
            }

            jobStatus.put("totalItems", paymentsToExport.size());

            // Generar exportación según formato
            byte[] exportData;
            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                exportData = exportToExcel(paymentsToExport);
            } else {
                // Por defecto, CSV
                exportData = exportToCsv(paymentsToExport);
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");
            jobStatus.put("exportSize", exportData.length);

            return CompletableFuture.completedFuture(exportData);
        } catch (Exception e) {
            log.error("Error general en exportación de pagos: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> batchReconciliation(LocalDateTime cutoffDate) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "RECONCILIATION");
        batchStatus.put(batchId, jobStatus);

        try {
            // Obtener transacciones pendientes de reconciliación
            List<Transaction> transactions = transactionRepository.findTransactionsForReconciliation(cutoffDate);

            jobStatus.put("totalItems", transactions.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            List<String> reconciled = new ArrayList<>();
            List<String> failed = new ArrayList<>();

            for (Transaction tx : transactions) {
                try {
                    // Verificar estado en la pasarela
                    Transaction.TransactionStatus gatewayStatus =
                            paymentGatewayService.checkTransactionStatus(tx.getTransactionId());

                    if (gatewayStatus != tx.getStatus()) {
                        // Actualizar estado
                        tx.setStatus(gatewayStatus);

                        // Actualizar pago asociado si es necesario
                        if (tx.getPayment() != null) {
                            Payment payment = tx.getPayment();

                            if (gatewayStatus == Transaction.TransactionStatus.SUCCESSFUL) {
                                if (tx.getTransactionType() == Transaction.TransactionType.PAYMENT) {
                                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                                    payment.setPaymentDate(LocalDateTime.now());

                                    // Actualizar factura si existe
                                    if (payment.getInvoice() != null) {
                                        updateInvoiceAfterPayment(payment.getInvoice(), payment.getAmount());
                                    }
                                } else if (tx.getTransactionType() == Transaction.TransactionType.REFUND) {
                                    payment.setStatus(Payment.PaymentStatus.REFUNDED);
                                }
                            } else if (gatewayStatus == Transaction.TransactionStatus.FAILED) {
                                payment.setStatus(Payment.PaymentStatus.FAILED);
                            }

                            paymentRepository.save(payment);
                        }
                    }

                    // Marcar como reconciliada
                    tx.setReconciled(true);
                    tx.setReconciliationDate(LocalDateTime.now());
                    transactionRepository.save(tx);

                    reconciled.add(tx.getTransactionId());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al reconciliar transacción {}: {}", tx.getTransactionId(), e.getMessage());

                    failed.add(tx.getTransactionId());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Preparar resultado
            Map<String, Object> result = new HashMap<>();
            result.put("totalTransactions", transactions.size());
            result.put("reconciledTransactions", reconciled.size());
            result.put("failedTransactions", failed.size());
            result.put("reconciledIds", reconciled);
            result.put("failedIds", failed);

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");
            jobStatus.put("result", result);

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error general en reconciliación de transacciones: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public List<String> getActiveBatchJobs() {
        // Devuelve las claves de trabajos por lotes activos del mapa de estado
        return batchStatus.entrySet().stream()
                .filter(entry -> {
                    String status = (String) entry.getValue().getOrDefault("status", "");
                    return "PROCESSING".equals(status);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> reprocessFailedPayments(LocalDateTime startDate, LocalDateTime endDate) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "REPROCESS_FAILED");
        batchStatus.put(batchId, jobStatus);

        try {
            // También necesitamos agregar este método a PaymentRepository
            List<Payment> failedPayments = paymentRepository.findByStatusAndCreatedAtBetween(
                    Payment.PaymentStatus.FAILED, startDate, endDate);

            jobStatus.put("totalItems", failedPayments.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            Map<String, Object> result = new HashMap<>();
            result.put("totalPayments", failedPayments.size());
            List<Map<String, Object>> reprocessedPayments = new ArrayList<>();

            for (Payment payment : failedPayments) {
                try {
                    // Restablecer el estado del pago a PENDING
                    payment.setStatus(Payment.PaymentStatus.PENDING);
                    payment.setRetryCount(payment.getRetryCount() != null ? payment.getRetryCount() + 1 : 1);
                    payment.setLastRetryDate(LocalDateTime.now());
                    payment = paymentRepository.save(payment);

                    // Buscar un método de pago predeterminado para este cliente
                    Optional<PaymentMethod> defaultPaymentMethod =
                            paymentMethodRepository.findByCustomerNumberAndIsDefaultTrue(payment.getCustomerNumber());

                    if (defaultPaymentMethod.isPresent()) {
                        PaymentMethodDto paymentMethodDto = mapper.toDto(defaultPaymentMethod.get());

                        // Procesar el pago
                        Map<String, Object> reprocessInfo = new HashMap<>();
                        reprocessInfo.put("paymentNumber", payment.getPaymentNumber());
                        reprocessInfo.put("customerNumber", payment.getCustomerNumber());
                        reprocessInfo.put("amount", payment.getAmount());
                        reprocessInfo.put("status", "REPROCESSED");
                        reprocessInfo.put("timestamp", LocalDateTime.now());

                        reprocessedPayments.add(reprocessInfo);

                        // Actualizar estado del trabajo
                        jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                        jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                    } else {
                        // No hay método de pago predeterminado, marcar como fallido
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        payment.setFailureReason("No se encontró método de pago predeterminado para reprocesar");
                        paymentRepository.save(payment);

                        // Actualizar estado del trabajo
                        jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                        jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                    }
                } catch (Exception e) {
                    log.error("Error al reprocesar pago {}: {}", payment.getPaymentNumber(), e.getMessage());

                    // Actualizar estado del trabajo
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            result.put("reprocessedPayments", reprocessedPayments);
            result.put("successCount", jobStatus.get("successCount"));
            result.put("failureCount", jobStatus.get("failureCount"));

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error al reprocesar pagos fallidos: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error al reprocesar pagos: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> calculateBatchStatistics(List<String> paymentNumbers,
                                                                           List<Payment.PaymentStatus> statuses,
                                                                           LocalDateTime startDate,
                                                                           LocalDateTime endDate) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "CALCULATE_STATISTICS");
        batchStatus.put(batchId, jobStatus);

        try {
            // Preparar filtros
            List<Payment> payments = new ArrayList<>();

            if (paymentNumbers != null && !paymentNumbers.isEmpty()) {
                // Filtrar por números de pago
                for (String number : paymentNumbers) {
                    paymentRepository.findByPaymentNumber(number).ifPresent(payments::add);
                }
            } else if (statuses != null && !statuses.isEmpty()) {
                // Filtrar por estados
                for (Payment.PaymentStatus status : statuses) {
                    payments.addAll(paymentRepository.findByStatus(status));
                }
            } else if (startDate != null && endDate != null) {
                // Filtrar por rango de fechas
                payments.addAll(paymentRepository.findCompletedPaymentsWithinDateRange(startDate, endDate));
            } else {
                // Sin filtros, usar todos los pagos
                payments = paymentRepository.findAll();
            }

            // Calcular estadísticas
            Map<String, Object> statistics = new HashMap<>();

            // Total de pagos
            statistics.put("totalPayments", payments.size());

            // Pagos por estado
            Map<Payment.PaymentStatus, Long> paymentsByStatus = payments.stream()
                    .collect(Collectors.groupingBy(Payment::getStatus, Collectors.counting()));
            statistics.put("paymentsByStatus", paymentsByStatus);

            // Pagos por tipo
            Map<Payment.PaymentType, Long> paymentsByType = payments.stream()
                    .collect(Collectors.groupingBy(Payment::getPaymentType, Collectors.counting()));
            statistics.put("paymentsByType", paymentsByType);

            // Montos totales por moneda
            Map<String, BigDecimal> totalsByCurrency = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .collect(Collectors.groupingBy(
                            Payment::getCurrency,
                            Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                    ));
            statistics.put("totalsByCurrency", totalsByCurrency);

            // Top 10 clientes por monto total
            Map<String, BigDecimal> totalsByCustomer = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .collect(Collectors.groupingBy(
                            Payment::getCustomerNumber,
                            Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                    ));

            List<Map.Entry<String, BigDecimal>> topCustomers = totalsByCustomer.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            Map<String, BigDecimal> top10Customers = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> entry : topCustomers) {
                top10Customers.put(entry.getKey(), entry.getValue());
            }

            statistics.put("top10Customers", top10Customers);

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(statistics);
        } catch (Exception e) {
            log.error("Error general en cálculo de estadísticas: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> processAutoPayments() {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "AUTO_PAYMENTS");
        batchStatus.put(batchId, jobStatus);

        try {
            // Obtener facturas pendientes
            List<Invoice> pendingInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);

            jobStatus.put("totalItems", pendingInvoices.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            int processedCount = 0;

            for (Invoice invoice : pendingInvoices) {
                try {
                    // Verificar si el cliente tiene un método de pago predeterminado
                    // Aquí se implementaría la lógica para obtener el método de pago y procesar el pago automático
                    // Por ahora, solo simulamos la operación

                    // Marcar como pagada
                    invoice.setStatus(Invoice.InvoiceStatus.PAID);
                    invoice.setPaidAmount(invoice.getTotalAmount());
                    invoice.setPaymentDate(LocalDateTime.now());

                    invoiceRepository.save(invoice);
                    processedCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al procesar pago automático para factura {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(processedCount);
        } catch (Exception e) {
            log.error("Error general en procesamiento de pagos automáticos: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> generatePerformanceReport(LocalDateTime startDate, LocalDateTime endDate) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "PERFORMANCE_REPORT");
        batchStatus.put(batchId, jobStatus);

        try {
            Map<String, Object> report = new HashMap<>();

            // Total de transacciones
            List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate);
            report.put("totalTransactions", transactions.size());

            // Transacciones por estado
            Map<Transaction.TransactionStatus, Long> transactionsByStatus = transactions.stream()
                    .collect(Collectors.groupingBy(Transaction::getStatus, Collectors.counting()));
            report.put("transactionsByStatus", transactionsByStatus);

            // Tasa de éxito
            long successfulCount = transactions.stream()
                    .filter(tx -> tx.getStatus() == Transaction.TransactionStatus.SUCCESSFUL)
                    .count();

            double successRate = transactions.isEmpty() ? 0 : (double) successfulCount / transactions.size() * 100;
            report.put("successRate", Math.round(successRate * 100) / 100.0); // Redondear a 2 decimales

            // Tiempo promedio de procesamiento (en milisegundos)
            // Aquí se implementaría la lógica para calcular tiempos de procesamiento
            // Por ahora, se establece un valor fijo
            report.put("averageProcessingTimeMs", 250);

            // Errores más comunes
            Map<String, Long> errorCounts = transactions.stream()
                    .filter(tx -> tx.getStatus() == Transaction.TransactionStatus.FAILED && tx.getErrorCode() != null)
                    .collect(Collectors.groupingBy(Transaction::getErrorCode, Collectors.counting()));

            report.put("commonErrors", errorCounts);

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(report);
        } catch (Exception e) {
            log.error("Error general en generación de informe de rendimiento: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> updateOverduePayments() {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "UPDATE_OVERDUE");
        batchStatus.put(batchId, jobStatus);

        try {
            // Obtener pagos pendientes vencidos
            LocalDateTime now = LocalDateTime.now();
            List<Payment> overduePayments = paymentRepository.findPendingPaymentsWithDueDateBetween(
                    now.minusDays(30), now.minusDays(1));

            jobStatus.put("totalItems", overduePayments.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            int updatedCount = 0;

            for (Payment payment : overduePayments) {
                try {
                    // Marcar como fallido
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setUpdatedAt(LocalDateTime.now());

                    // Crear transacción de fallo
                    Transaction failTransaction = new Transaction();
                    failTransaction.setTransactionId(UUID.randomUUID().toString());
                    failTransaction.setTransactionType(Transaction.TransactionType.PAYMENT);
                    failTransaction.setAmount(payment.getAmount());
                    failTransaction.setCurrency(payment.getCurrency());
                    failTransaction.setStatus(Transaction.TransactionStatus.FAILED);
                    failTransaction.setTransactionDate(LocalDateTime.now());
                    failTransaction.setErrorCode("PAYMENT_OVERDUE");
                    failTransaction.setErrorDescription("Pago vencido automáticamente");
                    failTransaction.setPayment(payment);

                    transactionRepository.save(failTransaction);
                    paymentRepository.save(payment);

                    updatedCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al actualizar pago vencido {}: {}", payment.getPaymentNumber(), e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(updatedCount);
        } catch (Exception e) {
            log.error("Error general en actualización de pagos vencidos: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> notifyPendingPayments(int daysAhead) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "NOTIFY_PENDING");
        batchStatus.put(batchId, jobStatus);

        try {
            // Obtener pagos pendientes próximos a vencer
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime future = now.plusDays(daysAhead);

            List<Payment> pendingPayments = paymentRepository.findPendingPaymentsWithDueDateBetween(now, future);

            jobStatus.put("totalItems", pendingPayments.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            int notifiedCount = 0;

            // Aquí se implementaría la lógica para enviar notificaciones
            // Por ahora, solo simulamos la operación

            for (Payment payment : pendingPayments) {
                try {
                    log.info("Notificando sobre pago pendiente: {} para cliente: {}",
                            payment.getPaymentNumber(), payment.getCustomerNumber());

                    notifiedCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al notificar sobre pago pendiente {}: {}",
                            payment.getPaymentNumber(), e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(notifiedCount);
        } catch (Exception e) {
            log.error("Error general en notificación de pagos pendientes: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Map<String, Object> getBatchStatus(String batchId) {
        return batchStatus.getOrDefault(batchId, Map.of(
                "id", batchId,
                "status", "NOT_FOUND",
                "message", "No se encontró el trabajo con ID: " + batchId
        ));
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Integer> generateInvoicesForPayments(List<String> paymentNumbers) {
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("id", batchId);
        jobStatus.put("startTime", LocalDateTime.now());
        jobStatus.put("status", "PROCESSING");
        jobStatus.put("operation", "GENERATE_INVOICES");
        batchStatus.put(batchId, jobStatus);

        try {
            jobStatus.put("totalItems", paymentNumbers.size());
            jobStatus.put("processedItems", 0);
            jobStatus.put("successCount", 0);
            jobStatus.put("failureCount", 0);

            int generatedCount = 0;

            for (String paymentNumber : paymentNumbers) {
                try {
                    Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + paymentNumber));

                    // Verificar si ya tiene factura
                    if (payment.getInvoice() != null) {
                        // Ya tiene factura, omitir
                        continue;
                    }

                    // Crear factura
                    Invoice invoice = new Invoice();
                    invoice.setInvoiceNumber(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    invoice.setCustomerNumber(payment.getCustomerNumber());
                    invoice.setPolicyNumber(payment.getPolicyNumber());
                    invoice.setInvoiceType(Invoice.InvoiceType.PREMIUM);
                    invoice.setIssueDate(LocalDateTime.now());
                    invoice.setDueDate(LocalDateTime.now().plusDays(30));
                    invoice.setTotalAmount(payment.getAmount());
                    invoice.setCurrency(payment.getCurrency());
                    invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
                    invoice.setDescription("Factura generada automáticamente para pago: " + paymentNumber);
                    invoice.setCreatedAt(LocalDateTime.now());

                    // Calcular impuestos (ejemplo)
                    invoice.setTaxAmount(payment.getAmount().multiply(new BigDecimal("0.21")));
                    invoice.setNetAmount(payment.getAmount().subtract(invoice.getTaxAmount()));

                    Invoice savedInvoice = invoiceRepository.save(invoice);

                    // Asociar pago con factura
                    payment.setInvoice(savedInvoice);
                    paymentRepository.save(payment);

                    generatedCount++;

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("successCount", (int) jobStatus.get("successCount") + 1);
                } catch (Exception e) {
                    log.error("Error al generar factura para pago {}: {}", paymentNumber, e.getMessage());

                    // Actualizar estado
                    jobStatus.put("processedItems", (int) jobStatus.get("processedItems") + 1);
                    jobStatus.put("failureCount", (int) jobStatus.get("failureCount") + 1);
                }
            }

            // Actualizar estado final
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "COMPLETED");

            return CompletableFuture.completedFuture(generatedCount);
        } catch (Exception e) {
            log.error("Error general en generación de facturas: {}", e.getMessage());

            // Actualizar estado final con error
            jobStatus.put("endTime", LocalDateTime.now());
            jobStatus.put("status", "FAILED");
            jobStatus.put("errorMessage", e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    // Métodos privados auxiliares

    private byte[] exportToCsv(List<Payment> payments) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader("Número de Pago", "Cliente", "Póliza", "Factura", "Concepto",
                                "Monto", "Moneda", "Estado", "Fecha Creación", "Fecha Pago"))
        ) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Payment payment : payments) {
                csvPrinter.printRecord(
                        payment.getPaymentNumber(),
                        payment.getCustomerNumber(),
                        payment.getPolicyNumber(),
                        payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : "",
                        payment.getConcept(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getStatus(),
                        payment.getCreatedAt().format(formatter),
                        payment.getPaymentDate() != null ? payment.getPaymentDate().format(formatter) : ""
                );
            }

            csvPrinter.flush();
        }

        return outputStream.toByteArray();
    }

    private byte[] exportToExcel(List<Payment> payments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pagos");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Crear cabecera
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Número de Pago", "Cliente", "Póliza", "Factura", "Concepto",
                    "Monto", "Moneda", "Estado", "Fecha Creación", "Fecha Pago"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;

            for (Payment payment : payments) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(payment.getPaymentNumber());
                row.createCell(1).setCellValue(payment.getCustomerNumber());
                row.createCell(2).setCellValue(payment.getPolicyNumber() != null ? payment.getPolicyNumber() : "");
                row.createCell(3).setCellValue(payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : "");
                row.createCell(4).setCellValue(payment.getConcept());
                row.createCell(5).setCellValue(payment.getAmount().doubleValue());
                row.createCell(6).setCellValue(payment.getCurrency());
                row.createCell(7).setCellValue(payment.getStatus().name());
                row.createCell(8).setCellValue(payment.getCreatedAt().format(formatter));
                row.createCell(9).setCellValue(payment.getPaymentDate() != null ?
                        payment.getPaymentDate().format(formatter) : "");
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
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
}