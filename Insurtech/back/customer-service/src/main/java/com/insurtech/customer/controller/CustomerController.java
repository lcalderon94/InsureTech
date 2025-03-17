package com.insurtech.customer.controller;

import com.insurtech.customer.exception.CustomerNotFoundException;
import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer", description = "API para la gestión de clientes")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Crear un nuevo cliente", description = "Crea un nuevo cliente en el sistema")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto customerDto) {
        log.info("Creating customer");
        CustomerDto createdCustomer = customerService.createCustomer(customerDto);
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener cliente por ID", description = "Obtiene un cliente por su ID")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        log.info("Getting customer by ID: {}", id);
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + id));
    }

    @GetMapping("/number/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener cliente por número", description = "Obtiene un cliente por su número de cliente")
    public ResponseEntity<CustomerDto> getCustomerByNumber(@PathVariable String customerNumber) {
        log.info("Getting customer by number: {}", customerNumber);
        return customerService.getCustomerByNumber(customerNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener cliente por email", description = "Obtiene un cliente por su dirección de email")
    public ResponseEntity<CustomerDto> getCustomerByEmail(@PathVariable String email) {
        log.info("Getting customer by email: {}", email);
        return customerService.getCustomerByEmail(email)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con email: " + email));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar clientes", description = "Busca clientes por término de búsqueda")
    public ResponseEntity<Page<CustomerDto>> searchCustomers(
            @RequestParam String term,
            Pageable pageable) {
        log.info("Searching customers with term: {}", term);
        Page<CustomerDto> customers = customerService.searchCustomers(term, pageable);
        return ResponseEntity.ok(customers);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener todos los clientes", description = "Obtiene todos los clientes paginados")
    public ResponseEntity<Page<CustomerDto>> getAllCustomers(Pageable pageable) {
        log.info("Getting all customers");
        Page<CustomerDto> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza un cliente existente")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDto customerDto) {
        log.info("Updating customer with ID: {}", id);
        CustomerDto updatedCustomer = customerService.updateCustomer(id, customerDto);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente por su ID")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("Deleting customer with ID: {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado del cliente", description = "Actualiza el estado de un cliente")
    public ResponseEntity<CustomerDto> updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("Updating status to {} for customer with ID: {}", status, id);
        CustomerDto updatedCustomer = customerService.updateCustomerStatus(id, status);
        return ResponseEntity.ok(updatedCustomer);
    }

    @PostMapping("/{customerId}/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir cliente a segmento", description = "Añade un cliente a un segmento")
    public ResponseEntity<CustomerDto> addCustomerToSegment(
            @PathVariable Long customerId,
            @PathVariable Long segmentId) {
        log.info("Adding customer ID: {} to segment ID: {}", customerId, segmentId);
        CustomerDto updatedCustomer = customerService.addCustomerToSegment(customerId, segmentId);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{customerId}/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Eliminar cliente de segmento", description = "Elimina un cliente de un segmento")
    public ResponseEntity<CustomerDto> removeCustomerFromSegment(
            @PathVariable Long customerId,
            @PathVariable Long segmentId) {
        log.info("Removing customer ID: {} from segment ID: {}", customerId, segmentId);
        CustomerDto updatedCustomer = customerService.removeCustomerFromSegment(customerId, segmentId);
        return ResponseEntity.ok(updatedCustomer);
    }

    @GetMapping("/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener clientes por segmento", description = "Obtiene todos los clientes de un segmento")
    public ResponseEntity<List<CustomerDto>> getCustomersBySegment(@PathVariable Long segmentId) {
        log.info("Getting customers by segment ID: {}", segmentId);
        List<CustomerDto> customers = customerService.getCustomersBySegment(segmentId);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/check-email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Verificar email", description = "Verifica si un email ya está registrado")
    public ResponseEntity<Boolean> isEmailRegistered(@RequestParam String email) {
        log.info("Checking if email is registered: {}", email);
        boolean isRegistered = customerService.isEmailRegistered(email);
        return ResponseEntity.ok(isRegistered);
    }

    @GetMapping("/check-identification")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Verificar identificación", description = "Verifica si una identificación ya está registrada")
    public ResponseEntity<Boolean> isIdentificationRegistered(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType) {
        log.info("Checking if identification is registered: {}/{}", identificationNumber, identificationType);
        boolean isRegistered = customerService.isIdentificationRegistered(identificationNumber, identificationType);
        return ResponseEntity.ok(isRegistered);
    }
}