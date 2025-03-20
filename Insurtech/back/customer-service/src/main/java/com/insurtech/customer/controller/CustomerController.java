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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_AGENT')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto customerDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Usuario autenticado: {}", auth.getName());
        log.info("Creating customer");
        CustomerDto createdCustomer = customerService.createCustomer(customerDto);
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }

    // Mantener el endpoint por ID solo para compatibilidad interna y operaciones de sistema
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener cliente por ID (Solo sistema)", description = "Obtiene un cliente por su ID interno (uso restringido)")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        log.info("Getting customer by internal ID: {}", id);
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

    @GetMapping("/identification")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener cliente por identificación", description = "Obtiene un cliente por su tipo e identificación")
    public ResponseEntity<CustomerDto> getCustomerByIdentification(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType) {
        log.info("Getting customer by identification: {}/{}", identificationNumber, identificationType);
        return customerService.getCustomerByIdentification(identificationNumber, identificationType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Cliente no encontrado con identificación: " + identificationNumber + " (" + identificationType + ")"));
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

    // Modificar para usar número de cliente
    @PutMapping("/number/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza un cliente existente")
    public ResponseEntity<CustomerDto> updateCustomerByNumber(
            @PathVariable String customerNumber,
            @Valid @RequestBody CustomerDto customerDto) {
        log.info("Updating customer with number: {}", customerNumber);
        return customerService.getCustomerByNumber(customerNumber)
                .map(existingCustomer -> {
                    CustomerDto updatedCustomer = customerService.updateCustomer(existingCustomer.getId(), customerDto);
                    return ResponseEntity.ok(updatedCustomer);
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
    }

    // Modificar para usar email
    @PutMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar cliente por email", description = "Actualiza un cliente existente usando su email")
    public ResponseEntity<CustomerDto> updateCustomerByEmail(
            @PathVariable String email,
            @Valid @RequestBody CustomerDto customerDto) {
        log.info("Updating customer with email: {}", email);
        return customerService.getCustomerByEmail(email)
                .map(existingCustomer -> {
                    CustomerDto updatedCustomer = customerService.updateCustomer(existingCustomer.getId(), customerDto);
                    return ResponseEntity.ok(updatedCustomer);
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con email: " + email));
    }

    // Modificar para usar número de cliente
    @DeleteMapping("/number/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente por su número")
    public ResponseEntity<Void> deleteCustomerByNumber(@PathVariable String customerNumber) {
        log.info("Deleting customer with number: {}", customerNumber);
        return customerService.getCustomerByNumber(customerNumber)
                .map(existingCustomer -> {
                    customerService.deleteCustomer(existingCustomer.getId());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
    }

    // Modificar para usar número de cliente
    @PatchMapping("/number/{customerNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado del cliente", description = "Actualiza el estado de un cliente")
    public ResponseEntity<CustomerDto> updateCustomerStatusByNumber(
            @PathVariable String customerNumber,
            @RequestParam String status) {
        log.info("Updating status to {} for customer with number: {}", status, customerNumber);
        return customerService.getCustomerByNumber(customerNumber)
                .map(existingCustomer -> {
                    CustomerDto updatedCustomer = customerService.updateCustomerStatus(existingCustomer.getId(), status);
                    return ResponseEntity.ok(updatedCustomer);
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
    }

    // Modificar para usar número de cliente
    @PostMapping("/number/{customerNumber}/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir cliente a segmento", description = "Añade un cliente a un segmento")
    public ResponseEntity<CustomerDto> addCustomerToSegmentByNumber(
            @PathVariable String customerNumber,
            @PathVariable Long segmentId) {
        log.info("Adding customer number: {} to segment ID: {}", customerNumber, segmentId);
        return customerService.getCustomerByNumber(customerNumber)
                .map(existingCustomer -> {
                    CustomerDto updatedCustomer = customerService.addCustomerToSegment(existingCustomer.getId(), segmentId);
                    return ResponseEntity.ok(updatedCustomer);
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
    }

    // Modificar para usar número de cliente
    @DeleteMapping("/number/{customerNumber}/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Eliminar cliente de segmento", description = "Elimina un cliente de un segmento")
    public ResponseEntity<CustomerDto> removeCustomerFromSegmentByNumber(
            @PathVariable String customerNumber,
            @PathVariable Long segmentId) {
        log.info("Removing customer number: {} from segment ID: {}", customerNumber, segmentId);
        return customerService.getCustomerByNumber(customerNumber)
                .map(existingCustomer -> {
                    CustomerDto updatedCustomer = customerService.removeCustomerFromSegment(existingCustomer.getId(), segmentId);
                    return ResponseEntity.ok(updatedCustomer);
                })
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con número: " + customerNumber));
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