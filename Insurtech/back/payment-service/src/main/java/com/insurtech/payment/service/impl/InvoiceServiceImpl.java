package com.insurtech.payment.service.impl;

import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.client.PolicyServiceClient;
import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.InvoiceDto;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Invoice;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.repository.InvoiceRepository;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.InvoiceService;
import com.insurtech.payment.util.EntityDtoMapper;
import com.insurtech.payment.util.PaymentNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final EntityDtoMapper mapper;
    private final PaymentNumberGenerator numberGenerator;
    private final DistributedLockService lockService;
    private final CustomerServiceClient customerServiceClient;
    private final PolicyServiceClient policyServiceClient;

    @Override
    @Transactional
    public InvoiceDto createInvoice(InvoiceDto invoiceDto) {
        log.info("Creando nueva factura para cliente número: {}", invoiceDto.getCustomerNumber());

        // Validar existencia del cliente
        if (!customerServiceClient.customerExists(invoiceDto.getCustomerNumber()).getBody()) {
            throw new ResourceNotFoundException("Cliente no encontrado con número: " + invoiceDto.getCustomerNumber());
        }

        // Validar existencia de la póliza si se proporciona
        if (invoiceDto.getPolicyNumber() != null && !invoiceDto.getPolicyNumber().isEmpty()) {
            if (!policyServiceClient.policyExists(invoiceDto.getPolicyNumber()).getBody()) {
                throw new ResourceNotFoundException("Póliza no encontrada con número: " + invoiceDto.getPolicyNumber());
            }
        }

        // Crear entidad de factura
        Invoice invoice = mapper.toEntity(invoiceDto);
        invoice.setInvoiceNumber(numberGenerator.generateInvoiceNumber());
        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
        invoice.setCreatedAt(LocalDateTime.now());

        // Calcular importes si no se proporcionan
        if (invoice.getTaxAmount() == null) {
            // Por ejemplo, calcular 21% de IVA
            invoice.setTaxAmount(invoice.getTotalAmount().multiply(new BigDecimal("0.21")));
        }

        if (invoice.getNetAmount() == null) {
            invoice.setNetAmount(invoice.getTotalAmount().subtract(invoice.getTaxAmount()));
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Notificar al cliente sobre la nueva factura
        sendInvoiceNotification(savedInvoice, "CREATED");

        return mapper.toDto(savedInvoice);
    }

    public Map<String, Object> generateInvoiceAgingAnalysis() {
        Map<String, Object> agingAnalysis = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();

        // Categorizar facturas por antigüedad
        List<Invoice> allInvoices = invoiceRepository.findAll();

        Map<String, List<Invoice>> invoicesByAge = new HashMap<>();
        invoicesByAge.put("current", new ArrayList<>());
        invoicesByAge.put("1-30", new ArrayList<>());
        invoicesByAge.put("31-60", new ArrayList<>());
        invoicesByAge.put("61-90", new ArrayList<>());
        invoicesByAge.put("90+", new ArrayList<>());

        for (Invoice invoice : allInvoices) {
            if (invoice.getStatus() != Invoice.InvoiceStatus.PENDING &&
                    invoice.getStatus() != Invoice.InvoiceStatus.PARTIALLY_PAID &&
                    invoice.getStatus() != Invoice.InvoiceStatus.OVERDUE) {
                continue;
            }

            LocalDateTime dueDate = invoice.getDueDate();
            if (dueDate.isAfter(now)) {
                invoicesByAge.get("current").add(invoice);
            } else {
                long daysOverdue = ChronoUnit.DAYS.between(dueDate, now);
                if (daysOverdue <= 30) {
                    invoicesByAge.get("1-30").add(invoice);
                } else if (daysOverdue <= 60) {
                    invoicesByAge.get("31-60").add(invoice);
                } else if (daysOverdue <= 90) {
                    invoicesByAge.get("61-90").add(invoice);
                } else {
                    invoicesByAge.get("90+").add(invoice);
                }
            }
        }

        // Calcular importes para cada categoría
        Map<String, BigDecimal> amountsByAge = new HashMap<>();
        for (Map.Entry<String, List<Invoice>> entry : invoicesByAge.entrySet()) {
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(invoice -> {
                        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
                        return invoice.getTotalAmount().subtract(paidAmount);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            amountsByAge.put(entry.getKey(), totalAmount);
        }

        agingAnalysis.put("invoiceCountByAge", invoicesByAge.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
        agingAnalysis.put("amountsByAge", amountsByAge);

        return agingAnalysis;
    }

    @Async
    public CompletableFuture<Integer> sendInvoiceReminders(int daysBeforeDue, boolean includeOverdue) {
        int sentCount = 0;

        // Facturas próximas a vencer
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.plusDays(daysBeforeDue);

        List<Invoice> invoices = invoiceRepository.findInvoicesDueSoon(now, cutoffDate);

        // Incluir facturas vencidas si se solicita
        if (includeOverdue) {
            List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(now);
            invoices.addAll(overdueInvoices);
        }

        for (Invoice invoice : invoices) {
            try {
                // Enviar notificación al cliente
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "REMINDER");
                notification.put("customerNumber", invoice.getCustomerNumber());

                String title;
                String message;

                if (invoice.getDueDate().isBefore(now)) {
                    // Factura vencida
                    long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), now);
                    title = "Recordatorio: Factura vencida";
                    message = String.format("Su factura #%s por %s %s venció hace %d días. Por favor, regularice su pago.",
                            invoice.getInvoiceNumber(),
                            invoice.getTotalAmount().subtract(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO),
                            invoice.getCurrency(),
                            daysOverdue);
                } else {
                    // Factura próxima a vencer
                    long daysRemaining = ChronoUnit.DAYS.between(now, invoice.getDueDate());
                    title = "Recordatorio: Factura próxima a vencer";
                    message = String.format("Su factura #%s por %s %s vence en %d días. Por favor, programe su pago.",
                            invoice.getInvoiceNumber(),
                            invoice.getTotalAmount().subtract(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO),
                            invoice.getCurrency(),
                            daysRemaining);
                }

                notification.put("title", title);
                notification.put("message", message);
                notification.put("invoiceNumber", invoice.getInvoiceNumber());

                // Enviar notificación
                customerServiceClient.sendNotification("REMINDER-" + UUID.randomUUID().toString(), notification);
                sentCount++;

            } catch (Exception e) {
                log.error("Error al enviar recordatorio para factura {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(sentCount);
    }

    @Override
    public Optional<InvoiceDto> getInvoiceById(Long id) {
        return invoiceRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public Optional<InvoiceDto> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber).map(mapper::toDto);
    }

    @Override
    public List<InvoiceDto> getInvoicesByCustomerNumber(String customerNumber) {
        return invoiceRepository.findByCustomerNumber(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDto> getInvoicesByPolicyNumber(String policyNumber) {
        return invoiceRepository.findByPolicyNumber(policyNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDto> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<InvoiceDto> searchInvoices(String searchTerm, Pageable pageable) {
        return invoiceRepository.searchInvoices(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public InvoiceDto updateInvoice(Long id, InvoiceDto invoiceDto) {
        return invoiceRepository.findById(id)
                .map(existingInvoice -> {
                    // Actualizar campos editables
                    existingInvoice.setDescription(invoiceDto.getDescription());
                    existingInvoice.setDueDate(invoiceDto.getDueDate());
                    existingInvoice.setUpdatedAt(LocalDateTime.now());

                    return mapper.toDto(invoiceRepository.save(existingInvoice));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public InvoiceDto updateInvoiceStatus(String invoiceNumber, Invoice.InvoiceStatus status) {
        return lockService.executeWithLock("invoice_status_" + invoiceNumber, () -> {
            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

            // Validar transición de estado
            validateStatusTransition(invoice.getStatus(), status);

            // Actualizar estado
            invoice.setStatus(status);
            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Notificar al cliente sobre el cambio de estado
            sendInvoiceNotification(savedInvoice, "STATUS_UPDATED");

            return mapper.toDto(savedInvoice);
        });
    }

    @Override
    @Transactional
    public InvoiceDto markInvoiceAsPaid(String invoiceNumber, BigDecimal paidAmount, LocalDateTime paymentDate) {
        return lockService.executeWithLock("invoice_payment_" + invoiceNumber, () -> {
            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

            // Validar que la factura no esté ya pagada
            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                return mapper.toDto(invoice);
            }

            // Validar que el monto pagado sea suficiente
            if (paidAmount.compareTo(invoice.getTotalAmount()) < 0) {
                return registerPartialPayment(invoiceNumber, paidAmount, paymentDate);
            }

            // Marcar como pagada
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setPaidAmount(invoice.getTotalAmount());
            invoice.setPaymentDate(paymentDate);
            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Crear registro de pago si no existe
            createPaymentForInvoice(savedInvoice, paidAmount, paymentDate);

            // Notificar al cliente
            sendInvoiceNotification(savedInvoice, "PAID");

            return mapper.toDto(savedInvoice);
        });
    }

    @Override
    public byte[] generateInvoiceReport(LocalDateTime startDate, LocalDateTime endDate, String format) {
        // Obtener facturas en el rango de fechas
        List<Invoice> invoices = invoiceRepository.findByIssueDateBetween(startDate, endDate);

        // Generar informe según formato solicitado
        String reportContent = "Informe de facturas del " + startDate + " al " + endDate + "\n";
        reportContent += "Total de facturas: " + invoices.size() + "\n";

        BigDecimal totalAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reportContent += "Monto total: " + totalAmount + "\n\n";

        for (Invoice invoice : invoices) {
            reportContent += "Factura #" + invoice.getInvoiceNumber() +
                    " - Cliente: " + invoice.getCustomerNumber() +
                    " - Monto: " + invoice.getTotalAmount() +
                    " - Estado: " + invoice.getStatus() + "\n";
        }

        return reportContent.getBytes();
    }

    @Override
    @Transactional
    public InvoiceDto registerPartialPayment(String invoiceNumber, BigDecimal paidAmount, LocalDateTime paymentDate) {
        return lockService.executeWithLock("invoice_payment_" + invoiceNumber, () -> {
            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

            // Calcular nuevo monto pagado
            BigDecimal currentPaidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal newPaidAmount = currentPaidAmount.add(paidAmount);

            // Validar que no exceda el total
            if (newPaidAmount.compareTo(invoice.getTotalAmount()) > 0) {
                newPaidAmount = invoice.getTotalAmount();
            }

            // Actualizar factura
            invoice.setPaidAmount(newPaidAmount);

            // Actualizar estado según monto pagado
            if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(Invoice.InvoiceStatus.PAID);
                invoice.setPaymentDate(paymentDate);
            } else {
                invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
            }

            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Crear registro de pago
            createPaymentForInvoice(savedInvoice, paidAmount, paymentDate);

            // Notificar al cliente
            sendInvoiceNotification(savedInvoice, "PARTIAL_PAYMENT");

            return mapper.toDto(savedInvoice);
        });
    }

    @Override
    @Transactional
    public InvoiceDto cancelInvoice(String invoiceNumber, String reason) {
        return lockService.executeWithLock("invoice_cancel_" + invoiceNumber, () -> {
            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

            // Validar que la factura no esté ya pagada
            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                throw new IllegalStateException("No se puede cancelar una factura pagada");
            }

            // Cancelar factura
            invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
            invoice.setDescription(invoice.getDescription() + " | Cancelada: " + reason);
            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Notificar al cliente
            sendInvoiceNotification(savedInvoice, "CANCELLED");

            return mapper.toDto(savedInvoice);
        });
    }

    @Override
    public List<PaymentDto> getPaymentsByInvoiceNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

        return paymentRepository.findByInvoiceId(invoice.getId()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] generateInvoicePdf(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

        // Aquí se implementaría la lógica para generar el PDF
        // Por ahora, solo devolvemos un texto simple
        String invoiceContent = "FACTURA #" + invoice.getInvoiceNumber() + "\n";
        invoiceContent += "Fecha: " + invoice.getIssueDate() + "\n";
        invoiceContent += "Cliente: " + invoice.getCustomerNumber() + "\n";
        invoiceContent += "Póliza: " + invoice.getPolicyNumber() + "\n\n";
        invoiceContent += "Importe Neto: " + invoice.getNetAmount() + "\n";
        invoiceContent += "IVA: " + invoice.getTaxAmount() + "\n";
        invoiceContent += "Total: " + invoice.getTotalAmount() + "\n";
        invoiceContent += "Estado: " + invoice.getStatus() + "\n";

        return invoiceContent.getBytes();
    }

    @Override
    public void sendInvoiceByEmail(String invoiceNumber, String email) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con número: " + invoiceNumber));

        // Generar PDF
        byte[] pdfContent = generateInvoicePdf(invoiceNumber);

        // Preparar notificación
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "EMAIL");
        notification.put("to", email);
        notification.put("subject", "Factura #" + invoice.getInvoiceNumber());
        notification.put("body", "Adjunto encuentra su factura #" + invoice.getInvoiceNumber());
        notification.put("attachment", pdfContent);
        notification.put("attachmentName", "factura_" + invoice.getInvoiceNumber() + ".pdf");

        // Enviar notificación a través del servicio de clientes
        customerServiceClient.sendNotification("NOTIFICATION-" + UUID.randomUUID().toString(), notification);

        log.info("Factura {} enviada por email a {}", invoiceNumber, email);
    }

    @Override
    public List<InvoiceDto> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDto> getInvoicesDueSoon(int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(daysAhead);

        return invoiceRepository.findInvoicesDueSoon(now, future).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateOutstandingAmountForCustomer(String customerNumber) {
        return invoiceRepository.calculateTotalOutstandingForCustomer(customerNumber);
    }

    @Override
    public Map<String, Object> getInvoiceStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> statistics = new HashMap<>();

        // Obtener facturas emitidas en el período
        List<Invoice> invoices = invoiceRepository.findByIssueDateBetween(startDate, endDate);

        // Total de facturas
        statistics.put("totalInvoices", invoices.size());

        // Facturas por estado
        Map<Invoice.InvoiceStatus, Long> invoicesByStatus = invoices.stream()
                .collect(Collectors.groupingBy(Invoice::getStatus, Collectors.counting()));
        statistics.put("invoicesByStatus", invoicesByStatus);

        // Montos totales
        BigDecimal totalAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalAmount", totalAmount);

        BigDecimal totalPaid = invoices.stream()
                .filter(i -> i.getPaidAmount() != null)
                .map(Invoice::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalPaid", totalPaid);

        BigDecimal totalOutstanding = totalAmount.subtract(totalPaid);
        statistics.put("totalOutstanding", totalOutstanding);

        // Facturas vencidas
        long overdueCount = invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING || i.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID)
                .filter(i -> i.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        statistics.put("overdueInvoices", overdueCount);

        return statistics;
    }

    @Override
    @Async
    public CompletableFuture<Integer> processOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(LocalDateTime.now());

        int processedCount = 0;
        for (Invoice invoice : overdueInvoices) {
            try {
                // Actualizar estado a vencido
                invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);

                // Notificar al cliente
                sendInvoiceNotification(invoice, "OVERDUE");

                processedCount++;
            } catch (Exception e) {
                log.error("Error al procesar factura vencida {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(processedCount);
    }

    @Override
    @Async
    public CompletableFuture<List<InvoiceDto>> syncInvoicesWithExternalSystem(String externalSystemId) {
        // Aquí implementaríamos la sincronización con un sistema externo
        log.info("Sincronizando facturas con sistema externo: {}", externalSystemId);

        // Como ejemplo, solo devolvemos las facturas pendientes
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);

        return CompletableFuture.completedFuture(
                pendingInvoices.stream()
                        .map(mapper::toDto)
                        .collect(Collectors.toList())
        );
    }

    // Métodos privados auxiliares

    private void validateStatusTransition(Invoice.InvoiceStatus currentStatus, Invoice.InvoiceStatus newStatus) {
        // Validar transiciones permitidas
        boolean isValid = false;

        switch (currentStatus) {
            case ISSUED:
                isValid = (newStatus == Invoice.InvoiceStatus.PENDING ||
                        newStatus == Invoice.InvoiceStatus.CANCELLED);
                break;
            case PENDING:
                isValid = (newStatus == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                        newStatus == Invoice.InvoiceStatus.PAID ||
                        newStatus == Invoice.InvoiceStatus.OVERDUE ||
                        newStatus == Invoice.InvoiceStatus.CANCELLED);
                break;
            case PARTIALLY_PAID:
                isValid = (newStatus == Invoice.InvoiceStatus.PAID ||
                        newStatus == Invoice.InvoiceStatus.OVERDUE);
                break;
            case PAID:
                isValid = false; // Estado terminal
                break;
            case OVERDUE:
                isValid = (newStatus == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                        newStatus == Invoice.InvoiceStatus.PAID ||
                        newStatus == Invoice.InvoiceStatus.CANCELLED);
                break;
            case CANCELLED:
                isValid = false; // Estado terminal
                break;
        }

        if (!isValid) {
            throw new IllegalStateException(
                    "Transición de estado inválida: de " + currentStatus + " a " + newStatus);
        }
    }

    private void createPaymentForInvoice(Invoice invoice, BigDecimal amount, LocalDateTime paymentDate) {
        // Verificar si ya existe un pago para esta factura con la misma fecha
        boolean paymentExists = paymentRepository.findByInvoiceId(invoice.getId()).stream()
                .anyMatch(p -> p.getPaymentDate() != null && p.getPaymentDate().toLocalDate().equals(paymentDate.toLocalDate()));

        if (!paymentExists) {
            Payment payment = new Payment();
            payment.setPaymentNumber(numberGenerator.generatePaymentNumber());
            payment.setCustomerNumber(invoice.getCustomerNumber());
            payment.setPolicyNumber(invoice.getPolicyNumber());
            payment.setPaymentType(Payment.PaymentType.PREMIUM);
            payment.setConcept("Pago de factura #" + invoice.getInvoiceNumber());
            payment.setAmount(amount);
            payment.setCurrency(invoice.getCurrency());
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentDate(paymentDate);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setInvoice(invoice);

            paymentRepository.save(payment);
        }
    }

    private void sendInvoiceNotification(Invoice invoice, String eventType) {
        try {
            // Preparar notificación
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SYSTEM");
            notification.put("customerNumber", invoice.getCustomerNumber());
            notification.put("title", "Actualización de Factura #" + invoice.getInvoiceNumber());

            String message;
            switch (eventType) {
                case "CREATED":
                    message = "Se ha emitido una nueva factura por " + invoice.getTotalAmount() + " " + invoice.getCurrency();
                    break;
                case "STATUS_UPDATED":
                    message = "El estado de su factura ha cambiado a " + invoice.getStatus();
                    break;
                case "PAID":
                    message = "Su factura ha sido pagada completamente";
                    break;
                case "PARTIAL_PAYMENT":
                    message = "Se ha registrado un pago parcial en su factura. Importe pendiente: " +
                            invoice.getTotalAmount().subtract(invoice.getPaidAmount()) + " " + invoice.getCurrency();
                    break;
                case "CANCELLED":
                    message = "Su factura ha sido cancelada";
                    break;
                case "OVERDUE":
                    message = "Su factura está vencida. Por favor, regularice el pago a la brevedad";
                    break;
                default:
                    message = "Actualización de su factura";
            }

            notification.put("message", message);
            notification.put("invoiceNumber", invoice.getInvoiceNumber());

            // Enviar notificación
            customerServiceClient.sendNotification("NOTIFICATION-" + UUID.randomUUID().toString(), notification);
        } catch (Exception e) {
            log.error("Error al enviar notificación de factura: {}", e.getMessage());
        }
    }
}