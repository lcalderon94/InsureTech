package com.insurtech.payment.controller;

import com.insurtech.payment.client.ClaimServiceClient;
import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.client.PolicyServiceClient;
import com.insurtech.payment.model.dto.InvoiceDto;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.RefundDto;
import com.insurtech.payment.model.entity.Invoice;
import com.insurtech.payment.service.InvoiceService;
import com.insurtech.payment.service.PaymentService;
import com.insurtech.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/financial")
@Tag(name = "Información Financiera Integrada", description = "API para obtener información financiera integrada")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class FinancialIntegrationController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final RefundService refundService;
    private final CustomerServiceClient customerServiceClient;
    private final PolicyServiceClient policyServiceClient;
    private final ClaimServiceClient claimServiceClient;

    @GetMapping("/customer/{customerNumber}/profile")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Perfil financiero del cliente", description = "Obtiene perfil financiero completo del cliente")
    public ResponseEntity<Map<String, Object>> getCustomerFinancialProfile(@PathVariable String customerNumber) {
        log.info("Obteniendo perfil financiero para cliente: {}", customerNumber);

        Map<String, Object> profile = new HashMap<>();

        // Información del cliente
        ResponseEntity<Map<String, Object>> customerResponse = customerServiceClient.getCustomerByNumber(customerNumber);
        if (customerResponse.getStatusCode().is2xxSuccessful() && customerResponse.getBody() != null) {
            profile.put("customerInfo", customerResponse.getBody());
        }

        // Pólizas del cliente
        ResponseEntity<Object> policiesResponse = policyServiceClient.getPoliciesByCustomerNumber(customerNumber);
        if (policiesResponse.getStatusCode().is2xxSuccessful() && policiesResponse.getBody() != null) {
            profile.put("policies", policiesResponse.getBody());
        }

        // Estadísticas de pagos
        Map<String, Object> paymentStats = paymentService.getPaymentStatistics(customerNumber);
        profile.put("paymentStatistics", paymentStats);

        // Facturas pendientes
        List<InvoiceDto> pendingInvoices = invoiceService.getInvoicesByCustomerNumber(customerNumber)
                .stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING ||
                        i.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                        i.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .collect(Collectors.toList());
        profile.put("pendingInvoices", pendingInvoices);

        // Monto total pendiente
        BigDecimal outstandingAmount = invoiceService.calculateOutstandingAmountForCustomer(customerNumber);
        profile.put("totalOutstandingAmount", outstandingAmount);

        // Historial de reembolsos
        List<RefundDto> refunds = refundService.getRefundsByCustomerNumber(customerNumber);
        profile.put("refundHistory", refunds);

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/policy/{policyNumber}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Historial financiero de póliza", description = "Obtiene historial financiero completo de una póliza")
    public ResponseEntity<Map<String, Object>> getPolicyFinancialHistory(@PathVariable String policyNumber) {
        log.info("Obteniendo historial financiero para póliza: {}", policyNumber);

        Map<String, Object> history = new HashMap<>();

        // Información de la póliza
        ResponseEntity<Map<String, Object>> policyResponse = policyServiceClient.getPolicyByNumber(policyNumber);
        if (policyResponse.getStatusCode().is2xxSuccessful() && policyResponse.getBody() != null) {
            history.put("policyInfo", policyResponse.getBody());
        }

        // Pagos de la póliza
        List<PaymentDto> payments = paymentService.getPaymentsByPolicyNumber(policyNumber);
        history.put("payments", payments);

        // Facturas de la póliza
        List<InvoiceDto> invoices = invoiceService.getInvoicesByPolicyNumber(policyNumber);
        history.put("invoices", invoices);

        // Reembolsos de la póliza
        List<RefundDto> refunds = refundService.getRefundsByPolicyNumber(policyNumber);
        history.put("refunds", refunds);

        // Reclamaciones de la póliza con pagos asociados
        ResponseEntity<Object> claimsResponse = claimServiceClient.getClaimsByPolicyNumber(policyNumber);
        if (claimsResponse.getStatusCode().is2xxSuccessful() && claimsResponse.getBody() != null) {
            history.put("claims", claimsResponse.getBody());
        }

        // Calcular totales
        BigDecimal totalPaid = paymentService.calculateTotalPaidForPolicy(policyNumber);
        history.put("totalPaid", totalPaid);

        BigDecimal totalRefunded = refundService.calculateTotalRefundedForPolicy(policyNumber);
        history.put("totalRefunded", totalRefunded);

        return ResponseEntity.ok(history);
    }
}