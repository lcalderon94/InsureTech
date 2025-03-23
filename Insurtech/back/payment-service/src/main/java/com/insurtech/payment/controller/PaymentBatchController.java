package com.insurtech.payment.controller;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.PaymentBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para operaciones por lotes sobre pagos
 */
@RestController
@RequestMapping("/api/payments/batch")
@Tag(name = "Operaciones por Lotes", description = "API para operaciones por lotes sobre pagos")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PaymentBatchController {

    private static final Logger log = LoggerFactory.getLogger(PaymentBatchController.class);

    private final PaymentBatchService batchService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar lote de pagos", description = "Procesa un lote de pagos asíncronamente")
    public ResponseEntity<String> processBatch(@Valid @RequestBody List<PaymentDto> payments) {
        log.info("Procesando lote de {} pagos", payments.size());

        CompletableFuture<List<PaymentDto>> future = batchService.processBatch(payments);

        return new ResponseEntity<>("Procesamiento de lote iniciado con " + payments.size() + " pagos",
                HttpStatus.ACCEPTED);
    }

    @PostMapping("/reconcile-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reconciliación masiva de todos los pagos", description = "Reconcilia todos los pagos con sistemas externos")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> reconcileAllPayments(
            @RequestParam(defaultValue = "30") int cutoffDays) {
        log.info("Iniciando reconciliación masiva de todos los pagos de los últimos {} días", cutoffDays);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cutoffDays);
        return batchService.batchReconciliation(cutoffDate)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/analyze-payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Analizar efectividad de métodos de pago", description = "Analiza la tasa de éxito de diferentes métodos de pago")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> analyzePaymentMethodsEffectiveness() {
        log.info("Analizando efectividad de métodos de pago");
        return batchService.analyzePaymentMethodEffectiveness()
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @PostMapping("/scheduled-payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configurar pagos programados", description = "Configura procesamiento automático de pagos programados")
    public ResponseEntity<Map<String, Object>> configureScheduledPayments(
            @RequestBody Map<String, Object> schedule) {
        log.info("Configurando pagos programados con horario: {}", schedule);
        Map<String, Object> result = batchService.configureScheduledPayments(schedule);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cargar CSV de pagos", description = "Carga y procesa un archivo CSV con datos de pagos")
    public ResponseEntity<String> uploadPaymentsCsv(@RequestParam("file") MultipartFile file) {
        log.info("Cargando archivo CSV de pagos: {}", file.getOriginalFilename());

        try {
            CompletableFuture<List<PaymentDto>> future = batchService.processPaymentsFromCsv(file.getInputStream());

            return new ResponseEntity<>("Carga de archivo CSV iniciada: " + file.getOriginalFilename(),
                    HttpStatus.ACCEPTED);
        } catch (IOException e) {
            log.error("Error al leer el archivo CSV", e);
            return new ResponseEntity<>("Error al leer el archivo: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/status-update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar estado de pagos en lote", description = "Actualiza el estado de múltiples pagos a la vez")
    public ResponseEntity<String> updatePaymentStatusInBatch(
            @RequestBody List<String> paymentNumbers,
            @RequestParam Payment.PaymentStatus status,
            @RequestParam String reason) {
        log.info("Actualizando estado a {} para {} pagos", status, paymentNumbers.size());

        CompletableFuture<Integer> future = batchService.batchStatusUpdate(paymentNumbers, status, reason);

        return new ResponseEntity<>("Actualización masiva de estado iniciada para " + paymentNumbers.size() + " pagos",
                HttpStatus.ACCEPTED);
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancelar pagos en lote", description = "Cancela múltiples pagos a la vez")
    public ResponseEntity<String> cancelPaymentsInBatch(
            @RequestBody List<String> paymentNumbers,
            @RequestParam String reason) {
        log.info("Cancelando {} pagos por motivo: {}", paymentNumbers.size(), reason);

        CompletableFuture<Integer> future = batchService.batchCancelPayments(paymentNumbers, reason);

        return new ResponseEntity<>("Cancelación masiva iniciada para " + paymentNumbers.size() + " pagos",
                HttpStatus.ACCEPTED);
    }

    @GetMapping("/exports")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Exportar pagos", description = "Exporta pagos a un formato específico (CSV, Excel, etc.)")
    public CompletableFuture<ResponseEntity<byte[]>> exportPayments(
            @RequestParam(required = false) List<String> paymentNumbers,
            @RequestParam(required = false) List<Payment.PaymentStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "csv") String format) {

        log.info("Exportando pagos en formato {}", format);

        return batchService.exportPayments(paymentNumbers, statuses, startDate, endDate, format)
                .thenApply(data -> {
                    String contentType;
                    String filename;

                    if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                        contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                        filename = "payments_export.xlsx";
                    } else {
                        contentType = "text/csv";
                        filename = "payments_export.csv";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .body(data);
                });
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reconciliación masiva", description = "Reconcilia masivamente pagos con transacciones externas")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batchReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffDate) {
        log.info("Iniciando reconciliación masiva para transacciones hasta: {}", cutoffDate);
        return batchService.batchReconciliation(cutoffDate)
                .thenApply(result -> ResponseEntity.ok(result));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estadísticas de pagos", description = "Calcula estadísticas sobre un conjunto de pagos")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> calculateBatchStatistics(
            @RequestParam(required = false) List<String> paymentNumbers,
            @RequestParam(required = false) List<Payment.PaymentStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Calculando estadísticas para un conjunto de pagos");

        return batchService.calculateBatchStatistics(paymentNumbers, statuses, startDate, endDate)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/auto-payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar pagos automáticos", description = "Procesa pagos automáticos pendientes")
    public CompletableFuture<ResponseEntity<Integer>> processAutoPayments() {
        log.info("Iniciando procesamiento de pagos automáticos");
        return batchService.processAutoPayments()
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @GetMapping("/report/performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Informe de rendimiento", description = "Genera un informe de rendimiento de pagos")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPerformanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando informe de rendimiento de pagos para el período: {} - {}", startDate, endDate);

        return batchService.generatePerformanceReport(startDate, endDate)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/update-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar pagos vencidos", description = "Actualiza los estados de pagos vencidos")
    public CompletableFuture<ResponseEntity<Integer>> updateOverduePayments() {
        log.info("Iniciando actualización de pagos vencidos");
        return batchService.updateOverduePayments()
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @PostMapping("/notify-pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Notificar pagos pendientes", description = "Notifica sobre pagos pendientes")
    public CompletableFuture<ResponseEntity<Integer>> notifyPendingPayments(
            @RequestParam(defaultValue = "3") int daysAhead) {
        log.info("Notificando sobre pagos pendientes en los próximos {} días", daysAhead);
        return batchService.notifyPendingPayments(daysAhead)
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @GetMapping("/status/{batchId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Consultar estado de lote", description = "Consulta el estado de un proceso por lotes")
    public ResponseEntity<Map<String, Object>> getBatchStatus(@PathVariable String batchId) {
        log.info("Consultando estado del lote: {}", batchId);
        Map<String, Object> status = batchService.getBatchStatus(batchId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/generate-invoices")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generar facturas para pagos", description = "Genera masivamente facturas para pagos")
    public CompletableFuture<ResponseEntity<Integer>> generateInvoicesForPayments(
            @RequestBody List<String> paymentNumbers) {
        log.info("Generando facturas para {} pagos", paymentNumbers.size());
        return batchService.generateInvoicesForPayments(paymentNumbers)
                .thenApply(count -> ResponseEntity.ok(count));
    }
}