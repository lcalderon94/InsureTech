package com.insurtech.payment.controller;

import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.RefundDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Refund;
import com.insurtech.payment.service.RefundService;
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
 * Controlador REST para operaciones relacionadas con reembolsos
 */
@RestController
@RequestMapping("/api/refunds")
@Tag(name = "Reembolsos", description = "API para la gestión de reembolsos")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Solicitar un reembolso", description = "Crea una solicitud de reembolso")
    public ResponseEntity<RefundDto> requestRefund(@Valid @RequestBody RefundDto refundDto) {
        log.info("Solicitud recibida para crear reembolso para cliente número: {}", refundDto.getCustomerNumber());
        RefundDto createdRefund = refundService.requestRefund(refundDto);
        return new ResponseEntity<>(createdRefund, HttpStatus.CREATED);
    }

    @PostMapping("/number/{refundNumber}/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Procesar un reembolso", description = "Procesa un reembolso pendiente")
    public ResponseEntity<RefundDto> processRefund(@PathVariable String refundNumber) {
        log.info("Procesando reembolso número: {}", refundNumber);
        RefundDto processedRefund = refundService.processRefund(refundNumber);
        return ResponseEntity.ok(processedRefund);
    }

    @PostMapping("/number/{refundNumber}/process-async")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Procesar un reembolso asíncronamente", description = "Procesa un reembolso pendiente de forma asíncrona")
    public CompletableFuture<ResponseEntity<RefundDto>> processRefundAsync(@PathVariable String refundNumber) {
        log.info("Procesando reembolso número: {} de forma asíncrona", refundNumber);
        return refundService.processRefundAsync(refundNumber)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/number/{refundNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reembolso por número", description = "Obtiene un reembolso por su número único")
    public ResponseEntity<RefundDto> getRefundByNumber(@PathVariable String refundNumber) {
        log.info("Obteniendo reembolso por número: {}", refundNumber);
        return refundService.getRefundByNumber(refundNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));
    }

    @GetMapping("/customer/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reembolsos por cliente", description = "Obtiene todos los reembolsos de un cliente")
    public ResponseEntity<List<RefundDto>> getRefundsByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo reembolsos para cliente número: {}", customerNumber);
        List<RefundDto> refunds = refundService.getRefundsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/policy/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reembolsos por póliza", description = "Obtiene todos los reembolsos de una póliza")
    public ResponseEntity<List<RefundDto>> getRefundsByPolicyNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo reembolsos para póliza número: {}", policyNumber);
        List<RefundDto> refunds = refundService.getRefundsByPolicyNumber(policyNumber);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener reembolsos por estado", description = "Obtiene todos los reembolsos en un estado específico")
    public ResponseEntity<List<RefundDto>> getRefundsByStatus(@PathVariable Refund.RefundStatus status) {
        log.info("Obteniendo reembolsos con estado: {}", status);
        List<RefundDto> refunds = refundService.getRefundsByStatus(status);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar reembolsos", description = "Busca reembolsos por término de búsqueda")
    public ResponseEntity<Page<RefundDto>> searchRefunds(
            @RequestParam String searchTerm,
            Pageable pageable) {
        log.info("Buscando reembolsos con término: {}", searchTerm);
        Page<RefundDto> refunds = refundService.searchRefunds(searchTerm, pageable);
        return ResponseEntity.ok(refunds);
    }

    @PutMapping("/number/{refundNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar reembolso", description = "Actualiza un reembolso existente")
    public ResponseEntity<RefundDto> updateRefund(
            @PathVariable String refundNumber,
            @Valid @RequestBody RefundDto refundDto) {
        log.info("Actualizando reembolso con número: {}", refundNumber);
        return refundService.getRefundByNumber(refundNumber)
                .map(existingRefund -> {
                    RefundDto updatedRefund = refundService.updateRefund(existingRefund.getId(), refundDto);
                    return ResponseEntity.ok(updatedRefund);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con número: " + refundNumber));
    }

    @PatchMapping("/number/{refundNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de reembolso", description = "Actualiza el estado de un reembolso")
    public ResponseEntity<RefundDto> updateRefundStatus(
            @PathVariable String refundNumber,
            @RequestParam Refund.RefundStatus status,
            @RequestParam(required = false) String reason) {
        log.info("Actualizando estado a {} para reembolso número: {}", status, refundNumber);
        RefundDto updatedRefund = refundService.updateRefundStatus(refundNumber, status, reason);
        return ResponseEntity.ok(updatedRefund);
    }

    @PostMapping("/number/{refundNumber}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Aprobar reembolso", description = "Aprueba un reembolso solicitado")
    public ResponseEntity<RefundDto> approveRefund(@PathVariable String refundNumber) {
        log.info("Aprobando reembolso número: {}", refundNumber);
        RefundDto approvedRefund = refundService.approveRefund(refundNumber);
        return ResponseEntity.ok(approvedRefund);
    }

    @PostMapping("/number/{refundNumber}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Rechazar reembolso", description = "Rechaza un reembolso solicitado")
    public ResponseEntity<RefundDto> rejectRefund(
            @PathVariable String refundNumber,
            @RequestParam String reason) {
        log.info("Rechazando reembolso número: {} por motivo: {}", refundNumber, reason);
        RefundDto rejectedRefund = refundService.rejectRefund(refundNumber, reason);
        return ResponseEntity.ok(rejectedRefund);
    }

    @PostMapping("/number/{refundNumber}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Completar reembolso manualmente", description = "Completa manualmente un reembolso")
    public ResponseEntity<RefundDto> completeRefund(
            @PathVariable String refundNumber,
            @RequestParam String externalReference) {
        log.info("Completando manualmente reembolso número: {} con referencia externa: {}", refundNumber, externalReference);
        RefundDto completedRefund = refundService.completeRefund(refundNumber, externalReference);
        return ResponseEntity.ok(completedRefund);
    }

    @PostMapping("/number/{refundNumber}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Cancelar reembolso", description = "Cancela un reembolso pendiente")
    public ResponseEntity<RefundDto> cancelRefund(
            @PathVariable String refundNumber,
            @RequestParam String reason) {
        log.info("Cancelando reembolso número: {} por motivo: {}", refundNumber, reason);
        RefundDto cancelledRefund = refundService.cancelRefund(refundNumber, reason);
        return ResponseEntity.ok(cancelledRefund);
    }

    @GetMapping("/number/{refundNumber}/transaction")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener transacción de reembolso", description = "Obtiene la transacción asociada a un reembolso")
    public ResponseEntity<TransactionDto> getRefundTransaction(@PathVariable String refundNumber) {
        log.info("Obteniendo transacción para reembolso número: {}", refundNumber);
        return refundService.getRefundTransaction(refundNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción no encontrada para reembolso: " + refundNumber));
    }

    @GetMapping("/customer/{customerNumber}/total-refunded")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Calcular total reembolsado a cliente", description = "Calcula el total reembolsado a un cliente")
    public ResponseEntity<BigDecimal> calculateTotalRefundedForCustomer(@PathVariable String customerNumber) {
        log.info("Calculando total reembolsado para cliente número: {}", customerNumber);
        BigDecimal totalRefunded = refundService.calculateTotalRefundedForCustomer(customerNumber);
        return ResponseEntity.ok(totalRefunded);
    }

    @GetMapping("/policy/{policyNumber}/total-refunded")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Calcular total reembolsado de póliza", description = "Calcula el total reembolsado para una póliza")
    public ResponseEntity<BigDecimal> calculateTotalRefundedForPolicy(@PathVariable String policyNumber) {
        log.info("Calculando total reembolsado para póliza número: {}", policyNumber);
        BigDecimal totalRefunded = refundService.calculateTotalRefundedForPolicy(policyNumber);
        return ResponseEntity.ok(totalRefunded);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener estadísticas de reembolsos", description = "Obtiene estadísticas de reembolsos para un período")
    public ResponseEntity<Map<String, Object>> getRefundStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Obteniendo estadísticas de reembolsos para el período: {} - {}", startDate, endDate);
        Map<String, Object> statistics = refundService.getRefundStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping(value = "/report", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Generar informe de reembolsos", description = "Genera un informe de reembolsos para un período")
    public ResponseEntity<byte[]> generateRefundReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        log.info("Generando informe de reembolsos en formato {} para el período: {} - {}", format, startDate, endDate);

        byte[] report = refundService.generateRefundReport(startDate, endDate, format);

        String contentType;
        String filename;

        if ("csv".equalsIgnoreCase(format)) {
            contentType = "text/csv";
            filename = "refund_report.csv";
        } else if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = "refund_report.xlsx";
        } else {
            contentType = "application/pdf";
            filename = "refund_report.pdf";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(report);
    }

    @PostMapping("/process-pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar reembolsos pendientes", description = "Procesa reembolsos pendientes de forma asíncrona")
    public CompletableFuture<ResponseEntity<Integer>> processPendingRefunds() {
        log.info("Iniciando procesamiento de reembolsos pendientes");
        return refundService.processPendingRefunds()
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @PostMapping("/notify-processed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Notificar reembolsos procesados", description = "Envía notificaciones sobre reembolsos procesados")
    public CompletableFuture<ResponseEntity<Integer>> notifyProcessedRefunds() {
        log.info("Enviando notificaciones sobre reembolsos procesados");
        return refundService.notifyProcessedRefunds()
                .thenApply(count -> ResponseEntity.ok(count));
    }
}