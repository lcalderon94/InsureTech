package com.insurtech.payment.controller;

import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.repository.TransactionRepository;
import com.insurtech.payment.service.PaymentBatchService;
import com.insurtech.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Administración de Pagos", description = "API para administración y monitoreo de pagos")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class PaymentAdminController {

    private final PaymentService paymentService;
    private final PaymentBatchService batchService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard administrativo", description = "Proporciona una vista consolidada del sistema de pagos")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        log.info("Generando dashboard administrativo de pagos");

        Map<String, Object> dashboard = new HashMap<>();

        // Estadísticas generales
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        // Pagos procesados hoy
        Map<String, Object> todayStats = paymentService.getPaymentStatisticsForPeriod(startOfDay, now);
        dashboard.put("today", todayStats);

        // Pagos procesados esta semana
        Map<String, Object> weekStats = paymentService.getPaymentStatisticsForPeriod(startOfWeek, now);
        dashboard.put("thisWeek", weekStats);

        // Pagos procesados este mes
        Map<String, Object> monthStats = paymentService.getPaymentStatisticsForPeriod(startOfMonth, now);
        dashboard.put("thisMonth", monthStats);

        // Estado de los trabajos por lotes
        List<String> batchJobs = batchService.getActiveBatchJobs();
        List<Map<String, Object>> batchStatuses = new ArrayList<>();

        for (String batchId : batchJobs) {
            Map<String, Object> status = batchService.getBatchStatus(batchId);
            batchStatuses.add(status);
        }

        dashboard.put("activeBatchJobs", batchStatuses);

        // Errores de transacción recientes
        List<Transaction> failedTransactions = transactionRepository.findTop10ByStatusOrderByTransactionDateDesc(
                Transaction.TransactionStatus.FAILED);

        List<Map<String, Object>> recentErrors = failedTransactions.stream()
                .map(t -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("transactionId", t.getTransactionId());
                    error.put("date", t.getTransactionDate());
                    error.put("errorCode", t.getErrorCode());
                    error.put("errorDescription", t.getErrorDescription());
                    error.put("amount", t.getAmount());
                    error.put("currency", t.getCurrency());
                    return error;
                })
                .collect(Collectors.toList());

        dashboard.put("recentErrors", recentErrors);

        // Estado de salud del sistema
        Map<String, Object> systemHealth = new HashMap<>();
        systemHealth.put("status", "UP");
        systemHealth.put("pendingTransactions", transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING));
        systemHealth.put("processingTransactions", transactionRepository.countByStatus(Transaction.TransactionStatus.PROCESSING));

        dashboard.put("systemHealth", systemHealth);

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/retry-policy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Política de reintentos", description = "Obtiene la configuración actual de reintentos")
    public ResponseEntity<Map<String, Object>> getRetryPolicy() {
        log.info("Obteniendo política de reintentos");

        Map<String, Object> retryPolicy = paymentService.getRetryPolicy();
        return ResponseEntity.ok(retryPolicy);
    }

    @PutMapping("/retry-policy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar política de reintentos", description = "Actualiza la configuración de reintentos")
    public ResponseEntity<Map<String, Object>> updateRetryPolicy(@RequestBody Map<String, Object> policy) {
        log.info("Actualizando política de reintentos: {}", policy);

        Map<String, Object> updatedPolicy = paymentService.updateRetryPolicy(policy);
        return ResponseEntity.ok(updatedPolicy);
    }

    @PostMapping("/reprocess-failed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reprocesar pagos fallidos", description = "Reprocesa pagos que fallaron en un período específico")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> reprocessFailedPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Reprocesando pagos fallidos del período: {} - {}", startDate, endDate);

        return batchService.reprocessFailedPayments(startDate, endDate)
                .thenApply(result -> ResponseEntity.ok(result));
    }
}