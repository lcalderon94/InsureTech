package com.insurtech.payment.controller;

import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.InvoiceDto;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Invoice;
import com.insurtech.payment.service.InvoiceService;
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
 * Controlador REST para operaciones relacionadas con facturas
 */
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Facturas", description = "API para la gestión de facturas")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Crear una nueva factura", description = "Crea una nueva factura")
    public ResponseEntity<InvoiceDto> createInvoice(@Valid @RequestBody InvoiceDto invoiceDto) {
        log.info("Solicitud recibida para crear factura");
        InvoiceDto createdInvoice = invoiceService.createInvoice(invoiceDto);
        return new ResponseEntity<>(createdInvoice, HttpStatus.CREATED);
    }

    @GetMapping("/analytics/aging")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Análisis de antigüedad de facturas", description = "Proporciona análisis de facturas por antigüedad")
    public ResponseEntity<Map<String, Object>> getInvoiceAgingAnalysis() {
        log.info("Generando análisis de antigüedad de facturas");
        Map<String, Object> agingAnalysis = invoiceService.generateInvoiceAgingAnalysis();
        return ResponseEntity.ok(agingAnalysis);
    }

    @PostMapping("/batch/remind")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar recordatorios de facturas", description = "Envía recordatorios para facturas pendientes")
    public CompletableFuture<ResponseEntity<Integer>> sendInvoiceReminders(
            @RequestParam(defaultValue = "7") int daysBeforeDue,
            @RequestParam(defaultValue = "true") boolean includeOverdue) {
        log.info("Enviando recordatorios de facturas a {} días del vencimiento, incluir vencidas: {}",
                daysBeforeDue, includeOverdue);
        return invoiceService.sendInvoiceReminders(daysBeforeDue, includeOverdue)
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener factura por número", description = "Obtiene una factura por su número único")
    public ResponseEntity<InvoiceDto> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("Obteniendo factura por número: {}", invoiceNumber);
        return invoiceService.getInvoiceByNumber(invoiceNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));
    }

    @GetMapping("/customer/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener facturas por cliente", description = "Obtiene todas las facturas de un cliente")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo facturas para cliente número: {}", customerNumber);
        List<InvoiceDto> invoices = invoiceService.getInvoicesByCustomerNumber(customerNumber);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/policy/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener facturas por póliza", description = "Obtiene todas las facturas de una póliza")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByPolicyNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo facturas para póliza número: {}", policyNumber);
        List<InvoiceDto> invoices = invoiceService.getInvoicesByPolicyNumber(policyNumber);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar facturas", description = "Busca facturas por término de búsqueda")
    public ResponseEntity<Page<InvoiceDto>> searchInvoices(
            @RequestParam String searchTerm,
            Pageable pageable) {
        log.info("Buscando facturas con término: {}", searchTerm);
        Page<InvoiceDto> invoices = invoiceService.searchInvoices(searchTerm, pageable);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener facturas por estado", description = "Obtiene todas las facturas en un estado específico")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByStatus(@PathVariable Invoice.InvoiceStatus status) {
        log.info("Obteniendo facturas con estado: {}", status);
        List<InvoiceDto> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    @PutMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar factura", description = "Actualiza una factura existente")
    public ResponseEntity<InvoiceDto> updateInvoice(
            @PathVariable String invoiceNumber,
            @Valid @RequestBody InvoiceDto invoiceDto) {
        log.info("Actualizando factura con número: {}", invoiceNumber);
        return invoiceService.getInvoiceByNumber(invoiceNumber)
                .map(existingInvoice -> {
                    InvoiceDto updatedInvoice = invoiceService.updateInvoice(existingInvoice.getId(), invoiceDto);
                    return ResponseEntity.ok(updatedInvoice);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));
    }

    @PatchMapping("/number/{invoiceNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de factura", description = "Actualiza el estado de una factura")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(
            @PathVariable String invoiceNumber,
            @RequestParam Invoice.InvoiceStatus status) {
        log.info("Actualizando estado a {} para factura número: {}", status, invoiceNumber);
        InvoiceDto updatedInvoice = invoiceService.updateInvoiceStatus(invoiceNumber, status);
        return ResponseEntity.ok(updatedInvoice);
    }

    @PatchMapping("/number/{invoiceNumber}/paid")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Marcar factura como pagada", description = "Marca una factura como pagada completamente")
    public ResponseEntity<InvoiceDto> markInvoiceAsPaid(
            @PathVariable String invoiceNumber,
            @RequestParam BigDecimal paidAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paymentDate) {
        log.info("Marcando factura {} como pagada con monto: {}", invoiceNumber, paidAmount);
        InvoiceDto updatedInvoice = invoiceService.markInvoiceAsPaid(invoiceNumber, paidAmount, paymentDate);
        return ResponseEntity.ok(updatedInvoice);
    }

    @PatchMapping("/number/{invoiceNumber}/partial-payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Registrar pago parcial", description = "Registra un pago parcial en una factura")
    public ResponseEntity<InvoiceDto> registerPartialPayment(
            @PathVariable String invoiceNumber,
            @RequestParam BigDecimal paidAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paymentDate) {
        log.info("Registrando pago parcial de {} para factura número: {}", paidAmount, invoiceNumber);
        InvoiceDto updatedInvoice = invoiceService.registerPartialPayment(invoiceNumber, paidAmount, paymentDate);
        return ResponseEntity.ok(updatedInvoice);
    }

    @PostMapping("/number/{invoiceNumber}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Cancelar factura", description = "Cancela una factura")
    public ResponseEntity<InvoiceDto> cancelInvoice(
            @PathVariable String invoiceNumber,
            @RequestParam String reason) {
        log.info("Cancelando factura número: {} por motivo: {}", invoiceNumber, reason);
        InvoiceDto cancelledInvoice = invoiceService.cancelInvoice(invoiceNumber, reason);
        return ResponseEntity.ok(cancelledInvoice);
    }

    @GetMapping("/number/{invoiceNumber}/payments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener pagos de factura", description = "Obtiene todos los pagos asociados a una factura")
    public ResponseEntity<List<PaymentDto>> getPaymentsByInvoiceNumber(@PathVariable String invoiceNumber) {
        log.info("Obteniendo pagos para factura número: {}", invoiceNumber);
        List<PaymentDto> payments = invoiceService.getPaymentsByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(payments);
    }

    @GetMapping(value = "/number/{invoiceNumber}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Generar PDF de factura", description = "Genera un PDF de una factura")
    public ResponseEntity<byte[]> generateInvoicePdf(@PathVariable String invoiceNumber) {
        log.info("Generando PDF para factura número: {}", invoiceNumber);
        byte[] pdfContent = invoiceService.generateInvoicePdf(invoiceNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"invoice-" + invoiceNumber + ".pdf\"")
                .body(pdfContent);
    }

    @PostMapping("/number/{invoiceNumber}/send-email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Enviar factura por email", description = "Envía una factura por correo electrónico")
    public ResponseEntity<Void> sendInvoiceByEmail(
            @PathVariable String invoiceNumber,
            @RequestParam String email) {
        log.info("Enviando factura número: {} por email a: {}", invoiceNumber, email);
        invoiceService.sendInvoiceByEmail(invoiceNumber, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener facturas vencidas", description = "Obtiene todas las facturas vencidas")
    public ResponseEntity<List<InvoiceDto>> getOverdueInvoices() {
        log.info("Obteniendo facturas vencidas");
        List<InvoiceDto> invoices = invoiceService.getOverdueInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/due-soon")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener facturas por vencer", description = "Obtiene facturas próximas a vencer")
    public ResponseEntity<List<InvoiceDto>> getInvoicesDueSoon(@RequestParam(defaultValue = "7") int daysAhead) {
        log.info("Obteniendo facturas por vencer en los próximos {} días", daysAhead);
        List<InvoiceDto> invoices = invoiceService.getInvoicesDueSoon(daysAhead);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/customer/{customerNumber}/outstanding")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Calcular monto pendiente", description = "Calcula el monto total pendiente para un cliente")
    public ResponseEntity<BigDecimal> calculateOutstandingAmountForCustomer(@PathVariable String customerNumber) {
        log.info("Calculando monto pendiente para cliente número: {}", customerNumber);
        BigDecimal outstandingAmount = invoiceService.calculateOutstandingAmountForCustomer(customerNumber);
        return ResponseEntity.ok(outstandingAmount);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener estadísticas de facturación", description = "Obtiene estadísticas de facturación para un período")
    public ResponseEntity<Map<String, Object>> getInvoiceStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Obteniendo estadísticas de facturación para el período: {} - {}", startDate, endDate);
        Map<String, Object> statistics = invoiceService.getInvoiceStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/process-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar facturas vencidas", description = "Procesa facturas vencidas de forma asíncrona")
    public CompletableFuture<ResponseEntity<Integer>> processOverdueInvoices() {
        log.info("Iniciando procesamiento de facturas vencidas");
        return invoiceService.processOverdueInvoices()
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sincronizar facturas", description = "Sincroniza facturas con un sistema externo")
    public CompletableFuture<ResponseEntity<List<InvoiceDto>>> syncInvoicesWithExternalSystem(
            @RequestParam String externalSystemId) {
        log.info("Sincronizando facturas con sistema externo: {}", externalSystemId);
        return invoiceService.syncInvoicesWithExternalSystem(externalSystemId)
                .thenApply(ResponseEntity::ok);
    }
}