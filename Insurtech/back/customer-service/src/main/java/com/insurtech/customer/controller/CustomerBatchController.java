package com.insurtech.customer.controller;

import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.service.CustomerBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/customers/batch")
@Tag(name = "Customer Batch", description = "API para operaciones por lotes sobre clientes")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerBatchController {

    private static final Logger log = LoggerFactory.getLogger(CustomerBatchController.class);

    private final CustomerBatchService customerBatchService;

    @Autowired
    public CustomerBatchController(CustomerBatchService customerBatchService) {
        this.customerBatchService = customerBatchService;
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar lote de clientes", description = "Procesa un lote de clientes asíncronamente")
    public ResponseEntity<String> processBatch(@Valid @RequestBody List<CustomerDto> customers) {
        log.info("Processing batch of {} customers", customers.size());

        Future<List<CustomerDto>> future = customerBatchService.processBatch(customers);

        return new ResponseEntity<>("Procesamiento de lote iniciado con " + customers.size() + " clientes",
                HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cargar CSV de clientes", description = "Carga y procesa un archivo CSV con datos de clientes")
    public ResponseEntity<String> uploadCustomersCsv(@RequestParam("file") MultipartFile file) {
        log.info("Uploading customer CSV file: {}", file.getOriginalFilename());

        try {
            Future<List<CustomerDto>> future = customerBatchService.processCustomersFromCsv(file.getInputStream());

            return new ResponseEntity<>("Carga de archivo CSV iniciada: " + file.getOriginalFilename(),
                    HttpStatus.ACCEPTED);
        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            return new ResponseEntity<>("Error al leer el archivo: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/segments/{segmentId}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar clientes por segmento", description = "Actualiza un campo para todos los clientes de un segmento")
    public ResponseEntity<String> updateCustomersBySegment(
            @PathVariable Long segmentId,
            @RequestParam String fieldName,
            @RequestParam String fieldValue) {
        log.info("Updating field '{}' to '{}' for customers in segment {}", fieldName, fieldValue, segmentId);

        Future<Integer> future = customerBatchService.updateCustomersBySegment(segmentId, fieldName, fieldValue);

        return new ResponseEntity<>("Actualización masiva iniciada para segmento " + segmentId,
                HttpStatus.ACCEPTED);
    }

    @GetMapping("/segments/{segmentId}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Estadísticas de segmento", description = "Calcula estadísticas para un segmento de clientes")
    public ResponseEntity<Map<String, Object>> calculateSegmentStatistics(@PathVariable Long segmentId)
            throws InterruptedException, ExecutionException {
        log.info("Calculating statistics for segment {}", segmentId);

        Future<Map<String, Object>> future = customerBatchService.calculateSegmentStatistics(segmentId);

        // Esperar hasta que se completen los cálculos
        Map<String, Object> statistics = future.get();

        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/validate-addresses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Validar direcciones", description = "Valida direcciones para un conjunto de clientes")
    public ResponseEntity<String> validateAddressesBatch(@RequestBody List<Long> customerIds) {
        log.info("Validating addresses for {} customers", customerIds.size());

        Future<List<Long>> future = customerBatchService.validateAddressesBatch(customerIds);

        return new ResponseEntity<>("Validación de direcciones iniciada para " + customerIds.size() + " clientes",
                HttpStatus.ACCEPTED);
    }

    @GetMapping("/reports/{reportType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Generar informe", description = "Genera un informe para un conjunto de clientes")
    public ResponseEntity<byte[]> generateCustomerReport(
            @PathVariable String reportType,
            @RequestParam List<Long> customerIds) throws InterruptedException, ExecutionException {
        log.info("Generating {} report for {} customers", reportType, customerIds.size());

        Future<byte[]> future = customerBatchService.generateCustomerReport(reportType, customerIds);

        // Esperar hasta que se genere el informe
        byte[] reportContent = future.get();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(reportContent);
    }

    @GetMapping("/status/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Estado de trabajo", description = "Obtiene el estado de un trabajo por lotes")
    public ResponseEntity<String> getJobStatus(@PathVariable String jobId) {
        // Este endpoint es un placeholder para consultar el estado de trabajos por lotes
        // En una implementación real, se podría utilizar un sistema de seguimiento de trabajos
        log.info("Checking status for job ID: {}", jobId);

        return ResponseEntity.ok("El estado actual del trabajo " + jobId + " es: EN PROGRESO");
    }
}