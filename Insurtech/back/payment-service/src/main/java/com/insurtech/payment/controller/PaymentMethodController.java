package com.insurtech.payment.controller;

import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para operaciones relacionadas con métodos de pago
 */
@RestController
@RequestMapping("/api/payment-methods")
@Tag(name = "Métodos de Pago", description = "API para la gestión de métodos de pago")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PaymentMethodController {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodController.class);

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Crear un nuevo método de pago", description = "Crea un nuevo método de pago para un cliente")
    public ResponseEntity<PaymentMethodDto> createPaymentMethod(@Valid @RequestBody PaymentMethodDto paymentMethodDto) {
        log.info("Solicitud recibida para crear método de pago para cliente número: {}", paymentMethodDto.getCustomerNumber());
        PaymentMethodDto createdPaymentMethod = paymentMethodService.createPaymentMethod(paymentMethodDto);
        return new ResponseEntity<>(createdPaymentMethod, HttpStatus.CREATED);
    }

    @GetMapping("/number/{paymentMethodNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener método de pago por número", description = "Obtiene un método de pago por su número único")
    public ResponseEntity<PaymentMethodDto> getPaymentMethodByNumber(@PathVariable String paymentMethodNumber) {
        log.info("Obteniendo método de pago por número: {}", paymentMethodNumber);
        return paymentMethodService.getPaymentMethodByNumber(paymentMethodNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));
    }

    @GetMapping("/customer/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener métodos de pago por cliente", description = "Obtiene todos los métodos de pago de un cliente")
    public ResponseEntity<List<PaymentMethodDto>> getPaymentMethodsByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo métodos de pago para cliente número: {}", customerNumber);
        List<PaymentMethodDto> paymentMethods = paymentMethodService.getPaymentMethodsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(paymentMethods);
    }

    @GetMapping("/customer/{customerNumber}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener métodos de pago activos por cliente", description = "Obtiene todos los métodos de pago activos de un cliente")
    public ResponseEntity<List<PaymentMethodDto>> getActivePaymentMethodsByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo métodos de pago activos para cliente número: {}", customerNumber);
        List<PaymentMethodDto> paymentMethods = paymentMethodService.getActivePaymentMethodsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(paymentMethods);
    }

    @GetMapping("/customer/{customerNumber}/default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener método de pago predeterminado", description = "Obtiene el método de pago predeterminado de un cliente")
    public ResponseEntity<PaymentMethodDto> getDefaultPaymentMethod(@PathVariable String customerNumber) {
        log.info("Obteniendo método de pago predeterminado para cliente número: {}", customerNumber);
        return paymentMethodService.getDefaultPaymentMethodByCustomerNumber(customerNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago predeterminado no encontrado para cliente: " + customerNumber));
    }

    @GetMapping("/type/{methodType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener métodos de pago por tipo", description = "Obtiene todos los métodos de pago de un tipo específico")
    public ResponseEntity<List<PaymentMethodDto>> getPaymentMethodsByType(
            @PathVariable PaymentMethod.MethodType methodType) {
        log.info("Obteniendo métodos de pago de tipo: {}", methodType);
        List<PaymentMethodDto> paymentMethods = paymentMethodService.getPaymentMethodsByType(methodType);
        return ResponseEntity.ok(paymentMethods);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar métodos de pago", description = "Busca métodos de pago por término de búsqueda")
    public ResponseEntity<Page<PaymentMethodDto>> searchPaymentMethods(
            @RequestParam String searchTerm,
            Pageable pageable) {
        log.info("Buscando métodos de pago con término: {}", searchTerm);
        Page<PaymentMethodDto> paymentMethods = paymentMethodService.searchPaymentMethods(searchTerm, pageable);
        return ResponseEntity.ok(paymentMethods);
    }

    @PutMapping("/number/{paymentMethodNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Actualizar método de pago", description = "Actualiza un método de pago existente")
    public ResponseEntity<PaymentMethodDto> updatePaymentMethod(
            @PathVariable String paymentMethodNumber,
            @Valid @RequestBody PaymentMethodDto paymentMethodDto) {
        log.info("Actualizando método de pago con número: {}", paymentMethodNumber);
        return paymentMethodService.getPaymentMethodByNumber(paymentMethodNumber)
                .map(existingMethod -> {
                    PaymentMethodDto updatedMethod = paymentMethodService.updatePaymentMethod(existingMethod.getId(), paymentMethodDto);
                    return ResponseEntity.ok(updatedMethod);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));
    }

    @PatchMapping("/number/{paymentMethodNumber}/card-details")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Actualizar detalles de tarjeta", description = "Actualiza los datos de una tarjeta de crédito/débito")
    public ResponseEntity<PaymentMethodDto> updateCardDetails(
            @PathVariable String paymentMethodNumber,
            @RequestParam String cardNumber,
            @RequestParam String cardExpiryMonth,
            @RequestParam String cardExpiryYear,
            @RequestParam String cvv) {
        log.info("Actualizando detalles de tarjeta para método de pago número: {}", paymentMethodNumber);
        PaymentMethodDto updatedMethod = paymentMethodService.updateCardDetails(
                paymentMethodNumber, cardNumber, cardExpiryMonth, cardExpiryYear, cvv);
        return ResponseEntity.ok(updatedMethod);
    }

    @PatchMapping("/number/{paymentMethodNumber}/set-default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Establecer método de pago predeterminado", description = "Establece un método de pago como predeterminado")
    public ResponseEntity<PaymentMethodDto> setDefaultPaymentMethod(@PathVariable String paymentMethodNumber) {
        log.info("Estableciendo método de pago número: {} como predeterminado", paymentMethodNumber);
        PaymentMethodDto updatedMethod = paymentMethodService.setDefaultPaymentMethod(paymentMethodNumber);
        return ResponseEntity.ok(updatedMethod);
    }

    @PatchMapping("/number/{paymentMethodNumber}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Activar método de pago", description = "Activa un método de pago")
    public ResponseEntity<PaymentMethodDto> activatePaymentMethod(@PathVariable String paymentMethodNumber) {
        log.info("Activando método de pago número: {}", paymentMethodNumber);
        PaymentMethodDto updatedMethod = paymentMethodService.activatePaymentMethod(paymentMethodNumber);
        return ResponseEntity.ok(updatedMethod);
    }

    @PatchMapping("/number/{paymentMethodNumber}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Desactivar método de pago", description = "Desactiva un método de pago")
    public ResponseEntity<PaymentMethodDto> deactivatePaymentMethod(@PathVariable String paymentMethodNumber) {
        log.info("Desactivando método de pago número: {}", paymentMethodNumber);
        PaymentMethodDto updatedMethod = paymentMethodService.deactivatePaymentMethod(paymentMethodNumber);
        return ResponseEntity.ok(updatedMethod);
    }

    @PostMapping("/number/{paymentMethodNumber}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Validar método de pago", description = "Valida un método de pago realizando una transacción de prueba")
    public ResponseEntity<Boolean> validatePaymentMethod(@PathVariable String paymentMethodNumber) {
        log.info("Validando método de pago número: {}", paymentMethodNumber);
        boolean isValid = paymentMethodService.validatePaymentMethod(paymentMethodNumber);
        return ResponseEntity.ok(isValid);
    }

    @PatchMapping("/number/{paymentMethodNumber}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Marcar método de pago como verificado", description = "Marca un método de pago como verificado")
    public ResponseEntity<PaymentMethodDto> markPaymentMethodAsVerified(@PathVariable String paymentMethodNumber) {
        log.info("Marcando método de pago número: {} como verificado", paymentMethodNumber);
        PaymentMethodDto updatedMethod = paymentMethodService.markPaymentMethodAsVerified(paymentMethodNumber);
        return ResponseEntity.ok(updatedMethod);
    }

    @DeleteMapping("/number/{paymentMethodNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Eliminar método de pago", description = "Elimina un método de pago")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable String paymentMethodNumber) {
        log.info("Eliminando método de pago número: {}", paymentMethodNumber);
        paymentMethodService.deletePaymentMethod(paymentMethodNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener tarjetas por expirar", description = "Obtiene tarjetas que expiran en un mes específico")
    public ResponseEntity<List<PaymentMethodDto>> findCardsExpiringInMonth(
            @RequestParam int month,
            @RequestParam int year) {
        log.info("Buscando tarjetas que expiran en {}/{}", month, year);
        List<PaymentMethodDto> expiringCards = paymentMethodService.findCardsExpiringInMonth(month, year);
        return ResponseEntity.ok(expiringCards);
    }

    @PostMapping("/update-expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar métodos de pago expirados", description = "Actualiza masivamente métodos de pago expirados")
    public CompletableFuture<ResponseEntity<Integer>> updateExpiredPaymentMethods() {
        log.info("Iniciando actualización masiva de métodos de pago expirados");
        return paymentMethodService.updateExpiredPaymentMethods()
                .thenApply(count -> ResponseEntity.ok(count));
    }

    @PostMapping("/notify-expiring")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Notificar tarjetas por expirar", description = "Notifica sobre tarjetas próximas a expirar")
    public CompletableFuture<ResponseEntity<List<PaymentMethodDto>>> notifyCardsExpiringSoon(
            @RequestParam(defaultValue = "30") int daysAhead) {
        log.info("Notificando sobre tarjetas que expiran en los próximos {} días", daysAhead);
        return paymentMethodService.notifyCardsExpiringSoon(daysAhead)
                .thenApply(ResponseEntity::ok);
    }
}