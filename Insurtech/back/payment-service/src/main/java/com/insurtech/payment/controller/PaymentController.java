package com.insurtech.payment.controller;

import com.insurtech.payment.exception.PaymentNotFoundException;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentRequestDto;
import com.insurtech.payment.model.dto.PaymentResponseDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para operaciones relacionadas con pagos
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos", description = "API para la gestión de pagos")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Crear un nuevo pago", description = "Crea un nuevo pago sin procesarlo")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody PaymentDto paymentDto) {
        log.info("Solicitud recibida para crear pago");
        PaymentDto createdPayment = paymentService.createPayment(paymentDto);
        return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Procesar un pago", description = "Crea y procesa un nuevo pago")
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        log.info("Solicitud recibida para procesar pago");
        PaymentResponseDto response = paymentService.processPayment(paymentRequestDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-async")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Procesar un pago asíncronamente", description = "Crea y procesa un nuevo pago de forma asíncrona")
    public CompletableFuture<ResponseEntity<PaymentResponseDto>> processPaymentAsync(
            @Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        log.info("Solicitud recibida para procesar pago asíncronamente");
        return paymentService.processPaymentAsync(paymentRequestDto)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/number/{paymentNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener pago por número", description = "Obtiene un pago por su número único")
    public ResponseEntity<PaymentDto> getPaymentByNumber(@PathVariable String paymentNumber) {
        log.info("Obteniendo pago por número: {}", paymentNumber);
        return paymentService.getPaymentByNumber(paymentNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));
    }

    @GetMapping("/customer/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener pagos por cliente", description = "Obtiene todos los pagos de un cliente")
    public ResponseEntity<List<PaymentDto>> getPaymentsByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo pagos para cliente número: {}", customerNumber);
        List<PaymentDto> payments = paymentService.getPaymentsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/policy/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener pagos por póliza", description = "Obtiene todos los pagos de una póliza")
    public ResponseEntity<List<PaymentDto>> getPaymentsByPolicyNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo pagos para póliza número: {}", policyNumber);
        List<PaymentDto> payments = paymentService.getPaymentsByPolicyNumber(policyNumber);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar pagos", description = "Busca pagos por término de búsqueda")
    public ResponseEntity<Page<PaymentDto>> searchPayments(
            @RequestParam String searchTerm,
            Pageable pageable) {
        log.info("Buscando pagos con término: {}", searchTerm);
        Page<PaymentDto> payments = paymentService.searchPayments(searchTerm, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener pagos por estado", description = "Obtiene todos los pagos en un estado específico")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStatus(@PathVariable Payment.PaymentStatus status) {
        log.info("Obteniendo pagos con estado: {}", status);
        List<PaymentDto> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    @PutMapping("/number/{paymentNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar pago", description = "Actualiza un pago existente")
    public ResponseEntity<PaymentDto> updatePayment(
            @PathVariable String paymentNumber,
            @Valid @RequestBody PaymentDto paymentDto) {
        log.info("Actualizando pago con número: {}", paymentNumber);
        return paymentService.getPaymentByNumber(paymentNumber)
                .map(existingPayment -> {
                    PaymentDto updatedPayment = paymentService.updatePayment(existingPayment.getId(), paymentDto);
                    return ResponseEntity.ok(updatedPayment);
                })
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con número: " + paymentNumber));
    }

    @PatchMapping("/number/{paymentNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de pago", description = "Actualiza el estado de un pago")
    public ResponseEntity<PaymentDto> updatePaymentStatus(
            @PathVariable String paymentNumber,
            @RequestParam Payment.PaymentStatus status,
            @RequestParam(required = false) String reason) {
        log.info("Actualizando estado a {} para pago número: {}", status, paymentNumber);
        PaymentDto updatedPayment = paymentService.updatePaymentStatus(paymentNumber, status, reason);
        return ResponseEntity.ok(updatedPayment);
    }

    @PostMapping("/number/{paymentNumber}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Cancelar pago", description = "Cancela un pago pendiente")
    public ResponseEntity<PaymentDto> cancelPayment(
            @PathVariable String paymentNumber,
            @RequestParam String reason) {
        log.info("Cancelando pago número: {} por motivo: {}", paymentNumber, reason);
        PaymentDto cancelledPayment = paymentService.cancelPayment(paymentNumber, reason);
        return ResponseEntity.ok(cancelledPayment);
    }

    @GetMapping("/number/{paymentNumber}/transactions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener transacciones de pago", description = "Obtiene todas las transacciones de un pago")
    public ResponseEntity<List<TransactionDto>> getTransactionsByPaymentNumber(@PathVariable String paymentNumber) {
        log.info("Obteniendo transacciones para pago número: {}", paymentNumber);
        List<TransactionDto> transactions = paymentService.getTransactionsByPaymentNumber(paymentNumber);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/number/{paymentNumber}/transactions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir transacción a pago", description = "Añade una nueva transacción a un pago existente")
    public ResponseEntity<TransactionDto> addTransactionToPayment(
            @PathVariable String paymentNumber,
            @Valid @RequestBody TransactionDto transactionDto) {
        log.info("Añadiendo transacción a pago número: {}", paymentNumber);
        TransactionDto createdTransaction = paymentService.createTransaction(paymentNumber, transactionDto);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @GetMapping("/policy/{policyNumber}/total-paid")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Calcular total pagado de póliza", description = "Calcula el total pagado para una póliza")
    public ResponseEntity<BigDecimal> calculateTotalPaidForPolicy(@PathVariable String policyNumber) {
        log.info("Calculando total pagado para póliza número: {}", policyNumber);
        BigDecimal totalPaid = paymentService.calculateTotalPaidForPolicy(policyNumber);
        return ResponseEntity.ok(totalPaid);
    }

    @GetMapping("/number/{paymentNumber}/is-completed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Verificar si un pago está completado", description = "Verifica si un pago ha sido completado")
    public ResponseEntity<Boolean> isPaymentCompleted(@PathVariable String paymentNumber) {
        log.info("Verificando si el pago número: {} está completado", paymentNumber);
        boolean isCompleted = paymentService.isPaymentCompleted(paymentNumber);
        return ResponseEntity.ok(isCompleted);
    }

    @GetMapping("/customer/{customerNumber}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener estadísticas de cliente", description = "Obtiene estadísticas de pagos para un cliente")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(@PathVariable String customerNumber) {
        log.info("Obteniendo estadísticas de pagos para cliente número: {}", customerNumber);
        Map<String, Object> statistics = paymentService.getPaymentStatistics(customerNumber);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener estadísticas de pagos", description = "Obtiene estadísticas de pagos para un período")
    public ResponseEntity<Map<String, Object>> getPaymentStatisticsForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Obteniendo estadísticas de pagos para el período: {} - {}", startDate, endDate);
        Map<String, Object> statistics = paymentService.getPaymentStatisticsForPeriod(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/due-soon")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener pagos por vencer", description = "Obtiene pagos pendientes próximos a vencer")
    public ResponseEntity<List<PaymentDto>> getPendingPaymentsDueSoon(@RequestParam(defaultValue = "7") int daysAhead) {
        log.info("Obteniendo pagos por vencer en los próximos {} días", daysAhead);
        List<PaymentDto> payments = paymentService.getPendingPaymentsDueSoon(daysAhead);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reconciliar pagos", description = "Reconcilia pagos con transacciones externas")
    public CompletableFuture<ResponseEntity<Integer>> reconcilePayments(@RequestBody List<String> externalReferences) {
        log.info("Reconciliando {} pagos con referencias externas", externalReferences.size());
        return paymentService.reconcilePayments(externalReferences)
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @GetMapping(value = "/report", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Generar informe de pagos", description = "Genera un informe de pagos para un período")
    public ResponseEntity<byte[]> generatePaymentReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        log.info("Generando informe de pagos en formato {} para el período: {} - {}", format, startDate, endDate);

        byte[] report = paymentService.generatePaymentReport(startDate, endDate, format);

        String contentType;
        String filename;

        if ("csv".equalsIgnoreCase(format)) {
            contentType = "text/csv";
            filename = "payment_report.csv";
        } else if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = "payment_report.xlsx";
        } else {
            contentType = "application/pdf";
            filename = "payment_report.pdf";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(report);
    }
}