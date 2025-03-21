package com.insurtech.payment.service.impl;

import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.exception.ResourceNotFoundException;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.repository.PaymentMethodRepository;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentGatewayService;
import com.insurtech.payment.service.PaymentMethodService;
import com.insurtech.payment.util.EntityDtoMapper;
import com.insurtech.payment.util.PaymentNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final EntityDtoMapper mapper;
    private final PaymentNumberGenerator numberGenerator;
    private final PaymentGatewayService paymentGatewayService;
    private final DistributedLockService lockService;
    private final CustomerServiceClient customerServiceClient;

    @Override
    @Transactional
    public PaymentMethodDto createPaymentMethod(PaymentMethodDto paymentMethodDto) {
        log.info("Creando nuevo método de pago para cliente número: {}", paymentMethodDto.getCustomerNumber());

        // Validar existencia del cliente
        if (!customerServiceClient.customerExists(paymentMethodDto.getCustomerNumber()).getBody()) {
            throw new ResourceNotFoundException("Cliente no encontrado con número: " + paymentMethodDto.getCustomerNumber());
        }

        // Crear entidad
        PaymentMethod paymentMethod = mapper.toEntity(paymentMethodDto);
        paymentMethod.setPaymentMethodNumber(numberGenerator.generatePaymentMethodNumber());
        paymentMethod.setCreatedAt(LocalDateTime.now());

        // Si es una tarjeta, enmascarar número y tokenizar
        if (paymentMethod.getMethodType() == PaymentMethod.MethodType.CREDIT_CARD ||
                paymentMethod.getMethodType() == PaymentMethod.MethodType.DEBIT_CARD) {

            if (paymentMethodDto.getFullCardNumber() != null) {
                // Enmascarar número de tarjeta
                String lastFour = paymentMethodDto.getFullCardNumber().substring(paymentMethodDto.getFullCardNumber().length() - 4);
                paymentMethod.setMaskedCardNumber("XXXX-XXXX-XXXX-" + lastFour);

                // Tokenizar tarjeta
                String token = paymentGatewayService.tokenizePaymentMethod(paymentMethodDto);
                paymentMethod.setPaymentToken(token);
                paymentMethod.setTokenExpiryDate(LocalDateTime.now().plusYears(1));
            }
        }

        // Si es método por defecto, desactivar otros
        if (paymentMethod.isDefault()) {
            setDefaultPaymentMethodIntern(paymentMethod.getCustomerNumber(), paymentMethod.getId());
        }

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);

        // Si es el primer método de pago, establecerlo como predeterminado
        if (paymentMethodRepository.findByCustomerNumber(paymentMethod.getCustomerNumber()).size() == 1) {
            savedMethod.setDefault(true);
            savedMethod = paymentMethodRepository.save(savedMethod);
        }

        return mapper.toDto(savedMethod);
    }

    @Override
    public Optional<PaymentMethodDto> getPaymentMethodById(Long id) {
        return paymentMethodRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public Optional<PaymentMethodDto> getPaymentMethodByNumber(String paymentMethodNumber) {
        return paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber).map(mapper::toDto);
    }

    @Override
    public List<PaymentMethodDto> getPaymentMethodsByCustomerNumber(String customerNumber) {
        return paymentMethodRepository.findByCustomerNumber(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentMethodDto> getActivePaymentMethodsByCustomerNumber(String customerNumber) {
        return paymentMethodRepository.findByCustomerNumberAndIsActiveTrue(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PaymentMethodDto> getDefaultPaymentMethodByCustomerNumber(String customerNumber) {
        return paymentMethodRepository.findByCustomerNumberAndIsDefaultTrue(customerNumber)
                .map(mapper::toDto);
    }

    @Override
    public List<PaymentMethodDto> getPaymentMethodsByType(PaymentMethod.MethodType methodType) {
        return paymentMethodRepository.findByMethodType(methodType).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentMethodDto> searchPaymentMethods(String searchTerm, Pageable pageable) {
        return paymentMethodRepository.searchPaymentMethods(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public PaymentMethodDto updatePaymentMethod(Long id, PaymentMethodDto paymentMethodDto) {
        return paymentMethodRepository.findById(id)
                .map(existingMethod -> {
                    // Actualizar campos editables
                    existingMethod.setName(paymentMethodDto.getName());
                    existingMethod.setUpdatedAt(LocalDateTime.now());

                    // Si cambia a método por defecto
                    if (paymentMethodDto.isDefault() && !existingMethod.isDefault()) {
                        existingMethod.setDefault(true);
                        setDefaultPaymentMethodIntern(existingMethod.getCustomerNumber(), existingMethod.getId());
                    }

                    return mapper.toDto(paymentMethodRepository.save(existingMethod));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public PaymentMethodDto updateCardDetails(String paymentMethodNumber, String cardNumber, String cardExpiryMonth,
                                              String cardExpiryYear, String cvv) {

        return lockService.executeWithLock("payment_method_" + paymentMethodNumber, () -> {
            PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

            // Validar que sea una tarjeta
            if (paymentMethod.getMethodType() != PaymentMethod.MethodType.CREDIT_CARD &&
                    paymentMethod.getMethodType() != PaymentMethod.MethodType.DEBIT_CARD) {
                throw new IllegalStateException("Solo se pueden actualizar detalles de tarjetas");
            }

            // Crear DTO para tokenización
            PaymentMethodDto methodDto = mapper.toDto(paymentMethod);
            methodDto.setFullCardNumber(cardNumber);
            methodDto.setCvv(cvv);

            // Actualizar fecha de expiración
            int month = Integer.parseInt(cardExpiryMonth);
            int year = Integer.parseInt(cardExpiryYear);
            YearMonth expiryDate = YearMonth.of(year, month);
            paymentMethod.setCardExpiryDate(expiryDate);

            // Enmascarar número
            paymentMethod.setMaskedCardNumber("XXXX-XXXX-XXXX-" + cardNumber.substring(cardNumber.length() - 4));

            // Tokenizar
            String token = paymentGatewayService.tokenizePaymentMethod(methodDto);
            paymentMethod.setPaymentToken(token);
            paymentMethod.setTokenExpiryDate(LocalDateTime.now().plusYears(1));

            paymentMethod.setUpdatedAt(LocalDateTime.now());

            PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
            return mapper.toDto(savedMethod);
        });
    }

    @Override
    @Transactional
    public PaymentMethodDto setDefaultPaymentMethod(String paymentMethodNumber) {
        return lockService.executeWithLock("payment_method_default_" + paymentMethodNumber, () -> {
            PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

            // Ya es el método por defecto
            if (paymentMethod.isDefault()) {
                return mapper.toDto(paymentMethod);
            }

            // Establecer como predeterminado
            paymentMethod.setDefault(true);
            setDefaultPaymentMethodIntern(paymentMethod.getCustomerNumber(), paymentMethod.getId());

            paymentMethod.setUpdatedAt(LocalDateTime.now());

            PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
            return mapper.toDto(savedMethod);
        });
    }

    @Override
    @Transactional
    public PaymentMethodDto activatePaymentMethod(String paymentMethodNumber) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

        // Ya está activo
        if (paymentMethod.isActive()) {
            return mapper.toDto(paymentMethod);
        }

        // Activar
        paymentMethod.setActive(true);
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        return mapper.toDto(savedMethod);
    }

    @Override
    @Transactional
    public PaymentMethodDto deactivatePaymentMethod(String paymentMethodNumber) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

        // Ya está inactivo
        if (!paymentMethod.isActive()) {
            return mapper.toDto(paymentMethod);
        }

        // Desactivar
        paymentMethod.setActive(false);
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        // Si era el método por defecto, seleccionar otro
        if (paymentMethod.isDefault()) {
            paymentMethod.setDefault(false);
            PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);

            // Buscar otro método activo para establecer como predeterminado
            Optional<PaymentMethod> anotherActiveMethod = paymentMethodRepository
                    .findByCustomerNumberAndIsActiveTrue(paymentMethod.getCustomerNumber())
                    .stream()
                    .findFirst();

            if (anotherActiveMethod.isPresent()) {
                anotherActiveMethod.get().setDefault(true);
                paymentMethodRepository.save(anotherActiveMethod.get());
            }

            return mapper.toDto(savedMethod);
        } else {
            PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
            return mapper.toDto(savedMethod);
        }
    }

    @Override
    public boolean validatePaymentMethod(String paymentMethodNumber) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

        PaymentMethodDto methodDto = mapper.toDto(paymentMethod);

        // Validar con la pasarela de pago
        boolean isValid = paymentGatewayService.validatePaymentMethod(methodDto);

        if (isValid) {
            // Marcar como verificado
            paymentMethod.setVerified(true);
            paymentMethod.setUpdatedAt(LocalDateTime.now());
            paymentMethodRepository.save(paymentMethod);
        }

        return isValid;
    }

    @Override
    @Transactional
    public PaymentMethodDto markPaymentMethodAsVerified(String paymentMethodNumber) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

        // Ya está verificado
        if (paymentMethod.isVerified()) {
            return mapper.toDto(paymentMethod);
        }

        // Marcar como verificado
        paymentMethod.setVerified(true);
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        return mapper.toDto(savedMethod);
    }

    @Override
    public String tokenizePaymentMethod(PaymentMethodDto paymentMethodDto) {
        return paymentGatewayService.tokenizePaymentMethod(paymentMethodDto);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(String paymentMethodNumber) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodNumber(paymentMethodNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con número: " + paymentMethodNumber));

        // Si es el método por defecto, buscar otro para establecer como predeterminado
        if (paymentMethod.isDefault()) {
            Optional<PaymentMethod> anotherMethod = paymentMethodRepository
                    .findByCustomerNumber(paymentMethod.getCustomerNumber())
                    .stream()
                    .filter(m -> !m.getPaymentMethodNumber().equals(paymentMethodNumber))
                    .findFirst();

            if (anotherMethod.isPresent()) {
                anotherMethod.get().setDefault(true);
                paymentMethodRepository.save(anotherMethod.get());
            }
        }

        paymentMethodRepository.delete(paymentMethod);
    }

    @Override
    public List<PaymentMethodDto> findCardsExpiringInMonth(int month, int year) {
        YearMonth expiryMonth = YearMonth.of(year, month);

        return paymentMethodRepository.findCardsByExpiryMonth(expiryMonth).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Async
    public CompletableFuture<Integer> updateExpiredPaymentMethods() {
        List<PaymentMethod> expiredCards = paymentMethodRepository.findExpiredCards(YearMonth.now());

        int deactivatedCount = 0;
        for (PaymentMethod method : expiredCards) {
            try {
                // Desactivar tarjeta expirada
                method.setActive(false);
                method.setUpdatedAt(LocalDateTime.now());
                paymentMethodRepository.save(method);

                // Notificar al cliente
                sendPaymentMethodNotification(method, "EXPIRED");

                deactivatedCount++;
            } catch (Exception e) {
                log.error("Error al actualizar método de pago expirado {}: {}", method.getPaymentMethodNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(deactivatedCount);
    }

    @Override
    @Async
    public CompletableFuture<List<PaymentMethodDto>> notifyCardsExpiringSoon(int daysAhead) {
        // Calcular mes de expiración objetivo
        YearMonth targetMonth = YearMonth.now().plusMonths(1);

        List<PaymentMethod> expiringCards = paymentMethodRepository.findCardsByExpiryMonth(targetMonth);

        for (PaymentMethod method : expiringCards) {
            try {
                // Notificar al cliente
                sendPaymentMethodNotification(method, "EXPIRING_SOON");
            } catch (Exception e) {
                log.error("Error al notificar sobre método de pago a expirar {}: {}",
                        method.getPaymentMethodNumber(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(
                expiringCards.stream()
                        .map(mapper::toDto)
                        .collect(Collectors.toList())
        );
    }

    // Métodos privados auxiliares

    private void setDefaultPaymentMethodIntern(String customerNumber, Long methodId) {
        // Buscar todos los métodos del cliente excepto el actual
        List<PaymentMethod> otherMethods = paymentMethodRepository.findByCustomerNumber(customerNumber).stream()
                .filter(m -> !m.getId().equals(methodId))
                .collect(Collectors.toList());

        // Quitar la marca de predeterminado de los demás
        for (PaymentMethod method : otherMethods) {
            method.setDefault(false);
            paymentMethodRepository.save(method);
        }
    }

    private void sendPaymentMethodNotification(PaymentMethod paymentMethod, String eventType) {
        try {
            // Preparar notificación
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SYSTEM");
            notification.put("customerNumber", paymentMethod.getCustomerNumber());

            String title;
            String message;

            switch (eventType) {
                case "EXPIRED":
                    title = "Método de pago expirado";
                    message = "Su método de pago " + paymentMethod.getName() + " ha expirado y ha sido desactivado.";
                    break;
                case "EXPIRING_SOON":
                    title = "Método de pago a expirar pronto";
                    message = "Su método de pago " + paymentMethod.getName() +
                            " expirará el " + paymentMethod.getCardExpiryDate() + ". Por favor, actualice sus datos.";
                    break;
                default:
                    title = "Actualización de método de pago";
                    message = "Ha habido una actualización en su método de pago " + paymentMethod.getName();
            }

            notification.put("title", title);
            notification.put("message", message);

            // Enviar notificación
            customerServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Error al enviar notificación de método de pago: {}", e.getMessage());
        }
    }
}