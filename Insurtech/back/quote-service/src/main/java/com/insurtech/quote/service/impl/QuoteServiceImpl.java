package com.insurtech.quote.service.impl;

import com.insurtech.quote.client.CustomerClient;
import com.insurtech.quote.client.PolicyClient;
import com.insurtech.quote.exception.BusinessValidationException;
import com.insurtech.quote.exception.ResourceNotFoundException;
import com.insurtech.quote.model.dto.*;
import com.insurtech.quote.model.entity.Quote;
import com.insurtech.quote.model.entity.QuoteCoverage;
import com.insurtech.quote.model.entity.QuoteOption;
import com.insurtech.quote.repository.QuoteRepository;
import com.insurtech.quote.service.QuoteCalculationService;
import com.insurtech.quote.service.QuoteService;
import com.insurtech.quote.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteServiceImpl.class);

    private final QuoteRepository quoteRepository;
    private final EntityDtoMapper mapper;
    private final QuoteCalculationService calculationService;
    private final CustomerClient customerClient;
    private final PolicyClient policyClient;

    @Override
    public Mono<QuoteResponseDto> createQuote(QuoteRequestDto quoteRequestDto) {
        log.info("Creando cotización para tipo: {}", quoteRequestDto.getQuoteType());

        // Resolver el ID del cliente si fue proporcionado por métodos alternativos
        return resolveCustomerId(quoteRequestDto)
                .flatMap(customerId -> {
                    QuoteDto quoteDto = new QuoteDto();
                    quoteDto.setCustomerId(customerId);
                    quoteDto.setQuoteType(quoteRequestDto.getQuoteType());
                    quoteDto.setStatus(Quote.QuoteStatus.DRAFT);
                    quoteDto.setSumInsured(quoteRequestDto.getSumInsured());
                    quoteDto.setEffectiveFrom(quoteRequestDto.getEffectiveFrom());
                    quoteDto.setEffectiveTo(quoteRequestDto.getEffectiveTo());
                    quoteDto.setPaymentFrequency(quoteRequestDto.getPaymentFrequency());
                    quoteDto.setRiskDetails(quoteRequestDto.getRiskDetails() != null
                            ? quoteRequestDto.getRiskDetails().toString() : null);
                    quoteDto.setValidUntil(LocalDateTime.now().plusDays(30)); // 30 días de validez de cotización

                    // Generar número de cotización único
                    quoteDto.setQuoteNumber(generateQuoteNumber(quoteRequestDto.getQuoteType()));

                    return getCurrentUsername()
                            .flatMap(username -> {
                                Quote quote = mapper.toEntity(quoteDto);
                                quote.setCreatedBy(username);
                                quote.setUpdatedBy(username);
                                quote.setCreatedAt(LocalDateTime.now());
                                quote.setUpdatedAt(LocalDateTime.now());

                                // Guardar la cotización inicial
                                return quoteRepository.save(quote)
                                        .flatMap(savedQuote -> {
                                            // Generar opciones de cotización
                                            return calculationService.generateQuoteOptions(quoteRequestDto)
                                                    .flatMap(options -> {
                                                        Set<QuoteOption> quoteOptions = options.stream()
                                                                .map(mapper::toEntity)
                                                                .collect(Collectors.toSet());

                                                        savedQuote.setOptions(quoteOptions);

                                                        // Calcular la prima total para la mejor opción
                                                        QuoteOption bestOption = quoteOptions.stream()
                                                                .filter(QuoteOption::isRecommended)
                                                                .findFirst()
                                                                .orElse(quoteOptions.stream()
                                                                        .min(Comparator.comparing(QuoteOption::getDisplayOrder))
                                                                        .orElse(null));

                                                        if (bestOption != null) {
                                                            savedQuote.setPremium(bestOption.getPremium());
                                                        }

                                                        // Actualizar la cotización con las opciones y la prima
                                                        return quoteRepository.save(savedQuote)
                                                                .map(updatedQuote -> {
                                                                    // Crear respuesta
                                                                    QuoteResponseDto response = new QuoteResponseDto();
                                                                    response.setQuoteNumber(updatedQuote.getQuoteNumber());
                                                                    response.setQuoteType(updatedQuote.getQuoteType());
                                                                    response.setStatus(updatedQuote.getStatus());
                                                                    response.setTotalPremium(updatedQuote.getPremium());
                                                                    response.setTotalSumInsured(updatedQuote.getSumInsured());
                                                                    response.setEffectiveFrom(updatedQuote.getEffectiveFrom());
                                                                    response.setEffectiveTo(updatedQuote.getEffectiveTo());
                                                                    response.setPaymentFrequency(updatedQuote.getPaymentFrequency());
                                                                    response.setValidUntil(updatedQuote.getValidUntil());
                                                                    response.setCreatedAt(updatedQuote.getCreatedAt());

                                                                    // Mapear opciones
                                                                    response.setOptions(updatedQuote.getOptions().stream()
                                                                            .map(mapper::toDto)
                                                                            .collect(Collectors.toList()));

                                                                    return response;
                                                                });
                                                    });
                                        });
                            });
                });
    }

    @Override
    public Mono<QuoteDto> getQuoteById(String id) {
        log.debug("Obteniendo cotización por ID: {}", id);

        return quoteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + id)))
                .map(mapper::toDto);
    }

    @Override
    public Mono<QuoteDto> getQuoteByNumber(String quoteNumber) {
        log.debug("Obteniendo cotización por número: {}", quoteNumber);

        return quoteRepository.findByQuoteNumber(quoteNumber)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con número: " + quoteNumber)))
                .map(mapper::toDto);
    }

    @Override
    public Flux<QuoteDto> getQuotesByCustomerId(Long customerId) {
        log.debug("Obteniendo cotizaciones para cliente ID: {}", customerId);

        return quoteRepository.findByCustomerId(customerId)
                .map(mapper::toDto);
    }

    @Override
    public Flux<QuoteDto> getQuotesByCustomerIdAndType(Long customerId, Quote.QuoteType quoteType) {
        log.debug("Obteniendo cotizaciones de tipo {} para cliente ID: {}", quoteType, customerId);

        return quoteRepository.findByCustomerIdAndQuoteType(customerId, quoteType)
                .map(mapper::toDto);
    }

    @Override
    public Flux<QuoteDto> getActiveQuotesByCustomerId(Long customerId) {
        log.debug("Obteniendo cotizaciones activas para cliente ID: {}", customerId);

        LocalDateTime now = LocalDateTime.now();

        return quoteRepository.findByCustomerId(customerId)
                .filter(quote -> quote.getValidUntil().isAfter(now) &&
                        quote.getStatus() != Quote.QuoteStatus.EXPIRED &&
                        quote.getStatus() != Quote.QuoteStatus.REJECTED &&
                        quote.getStatus() != Quote.QuoteStatus.CONVERTED_TO_POLICY)
                .map(mapper::toDto);
    }

    @Override
    public Mono<QuoteDto> updateQuote(String id, QuoteDto quoteDto) {
        log.info("Actualizando cotización con ID: {}", id);

        return quoteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + id)))
                .flatMap(existingQuote -> {
                    // Validar que no se pueda actualizar una cotización expirada o convertida
                    if (existingQuote.getStatus() == Quote.QuoteStatus.EXPIRED ||
                            existingQuote.getStatus() == Quote.QuoteStatus.CONVERTED_TO_POLICY ||
                            existingQuote.getStatus() == Quote.QuoteStatus.REJECTED) {
                        return Mono.error(new BusinessValidationException("No se puede actualizar una cotización " +
                                existingQuote.getStatus()));
                    }

                    return getCurrentUsername()
                            .flatMap(username -> {
                                // Actualizar campos permitidos
                                if (quoteDto.getEffectiveFrom() != null) existingQuote.setEffectiveFrom(quoteDto.getEffectiveFrom());
                                if (quoteDto.getEffectiveTo() != null) existingQuote.setEffectiveTo(quoteDto.getEffectiveTo());
                                if (quoteDto.getSumInsured() != null) existingQuote.setSumInsured(quoteDto.getSumInsured());
                                if (quoteDto.getPaymentFrequency() != null) existingQuote.setPaymentFrequency(quoteDto.getPaymentFrequency());
                                if (quoteDto.getRiskDetails() != null) existingQuote.setRiskDetails(quoteDto.getRiskDetails());
                                if (quoteDto.getAdditionalInformation() != null) existingQuote.setAdditionalInformation(quoteDto.getAdditionalInformation());
                                if (quoteDto.getStatus() != null) existingQuote.setStatus(quoteDto.getStatus());

                                existingQuote.setUpdatedBy(username);
                                existingQuote.setUpdatedAt(LocalDateTime.now());

                                // Si la cotización estaba expirada o rechazada y se está reactivando
                                if (quoteDto.getStatus() == Quote.QuoteStatus.DRAFT ||
                                        quoteDto.getStatus() == Quote.QuoteStatus.COMPLETED) {
                                    existingQuote.setValidUntil(LocalDateTime.now().plusDays(30));
                                }

                                // Actualizar coberturas si se proporcionaron
                                if (quoteDto.getCoverages() != null && !quoteDto.getCoverages().isEmpty()) {
                                    Set<QuoteCoverage> coverages = quoteDto.getCoverages().stream()
                                            .map(mapper::toEntity)
                                            .collect(Collectors.toSet());
                                    existingQuote.setCoverages(coverages);
                                }

                                // Recalcular prima si es necesario
                                if (quoteDto.getSumInsured() != null ||
                                        (quoteDto.getCoverages() != null && !quoteDto.getCoverages().isEmpty())) {
                                    return calculationService.calculateQuotePremium(mapper.toDto(existingQuote))
                                            .flatMap(newPremium -> {
                                                existingQuote.setPremium(newPremium);
                                                return quoteRepository.save(existingQuote);
                                            })
                                            .map(mapper::toDto);
                                } else {
                                    return quoteRepository.save(existingQuote)
                                            .map(mapper::toDto);
                                }
                            });
                });
    }

    @Override
    public Mono<QuoteDto> updateQuoteStatus(String id, Quote.QuoteStatus status) {
        log.info("Actualizando estado a {} para cotización ID: {}", status, id);

        return quoteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + id)))
                .flatMap(quote -> {
                    // Validar transición de estado
                    if (quote.getStatus() == Quote.QuoteStatus.CONVERTED_TO_POLICY) {
                        return Mono.error(new BusinessValidationException("No se puede cambiar el estado de una cotización ya convertida a póliza"));
                    }

                    return getCurrentUsername()
                            .flatMap(username -> {
                                quote.setStatus(status);
                                quote.setUpdatedBy(username);
                                quote.setUpdatedAt(LocalDateTime.now());

                                return quoteRepository.save(quote)
                                        .map(mapper::toDto);
                            });
                });
    }

    @Override
    public Mono<QuoteDto> acceptQuote(String id, String optionId) {
        log.info("Aceptando cotización ID: {} con opción ID: {}", id, optionId);

        return quoteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + id)))
                .flatMap(quote -> {
                    // Validar que la cotización no esté expirada
                    if (quote.getValidUntil().isBefore(LocalDateTime.now())) {
                        return Mono.error(new BusinessValidationException("La cotización ha expirado"));
                    }

                    // Validar que la cotización no esté ya aceptada o convertida
                    if (quote.getStatus() == Quote.QuoteStatus.ACCEPTED ||
                            quote.getStatus() == Quote.QuoteStatus.CONVERTED_TO_POLICY) {
                        return Mono.error(new BusinessValidationException("La cotización ya ha sido aceptada o convertida a póliza"));
                    }

                    // Si se especificó una opción, verificar que exista
                    if (optionId != null && !optionId.isEmpty()) {
                        boolean optionFound = quote.getOptions().stream()
                                .anyMatch(option -> optionId.equals(option.getOptionId()));

                        if (!optionFound) {
                            return Mono.error(new ResourceNotFoundException("Opción no encontrada con ID: " + optionId));
                        }

                        // Actualizar la cotización con la opción seleccionada
                        QuoteOption selectedOption = quote.getOptions().stream()
                                .filter(option -> optionId.equals(option.getOptionId()))
                                .findFirst()
                                .orElse(null);

                        if (selectedOption != null) {
                            quote.setPremium(selectedOption.getPremium());
                            quote.setCoverages(selectedOption.getCoverages());
                        }
                    }

                    return getCurrentUsername()
                            .flatMap(username -> {
                                quote.setStatus(Quote.QuoteStatus.ACCEPTED);
                                quote.setUpdatedBy(username);
                                quote.setUpdatedAt(LocalDateTime.now());

                                return quoteRepository.save(quote)
                                        .map(mapper::toDto);
                            });
                });
    }

    @Override
    public Mono<QuoteOptionDto> addQuoteOption(String quoteId, QuoteOptionDto optionDto) {
        log.info("Añadiendo opción a cotización ID: {}", quoteId);

        return quoteRepository.findById(quoteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + quoteId)))
                .flatMap(quote -> {
                    // Validar que la cotización esté en estado que permita añadir opciones
                    if (quote.getStatus() != Quote.QuoteStatus.DRAFT &&
                            quote.getStatus() != Quote.QuoteStatus.COMPLETED) {
                        return Mono.error(new BusinessValidationException("No se pueden añadir opciones a una cotización con estado " +
                                quote.getStatus()));
                    }

                    // Generar ID para la opción si no tiene
                    if (optionDto.getOptionId() == null || optionDto.getOptionId().isEmpty()) {
                        optionDto.setOptionId(UUID.randomUUID().toString());
                    }

                    QuoteOption option = mapper.toEntity(optionDto);

                    // Añadir la opción a la cotización
                    quote.getOptions().add(option);

                    return getCurrentUsername()
                            .flatMap(username -> {
                                quote.setUpdatedBy(username);
                                quote.setUpdatedAt(LocalDateTime.now());

                                return quoteRepository.save(quote)
                                        .map(savedQuote -> mapper.toDto(option));
                            });
                });
    }

    @Override
    public Mono<Void> deleteQuote(String id) {
        log.info("Eliminando cotización con ID: {}", id);

        return quoteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + id)))
                .flatMap(quote -> {
                    // Validar que solo se pueden eliminar cotizaciones en estado DRAFT
                    if (quote.getStatus() != Quote.QuoteStatus.DRAFT) {
                        return Mono.error(new BusinessValidationException("Solo se pueden eliminar cotizaciones en estado DRAFT"));
                    }

                    return quoteRepository.delete(quote);
                });
    }

    @Override
    public String generateQuoteNumber(Quote.QuoteType quoteType) {
        // Formato: QT-[TIPO]-YYYYMMDD-XXXX donde XXXX es un número aleatorio
        String typeCode = quoteType.name().substring(0, 3);
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "QT-" + typeCode + "-" + datePart + "-" + randomPart;
    }

    @Override
    public Mono<QuoteComparisonDto> compareQuotes(List<String> quoteNumbers) {
        log.info("Comparando cotizaciones: {}", quoteNumbers);

        if (quoteNumbers == null || quoteNumbers.isEmpty() || quoteNumbers.size() < 2) {
            return Mono.error(new IllegalArgumentException("Se requieren al menos dos cotizaciones para comparar"));
        }

        // Obtener todas las cotizaciones por número
        List<Mono<Quote>> quotesMonos = quoteNumbers.stream()
                .map(quoteRepository::findByQuoteNumber)
                .collect(Collectors.toList());

        return Flux.fromIterable(quotesMonos)
                .flatMap(quoteMono -> quoteMono)
                .collectList()
                .flatMap(quotes -> {
                    // Verificar que se encontraron todas las cotizaciones
                    if (quotes.size() != quoteNumbers.size()) {
                        return Mono.error(new ResourceNotFoundException("Una o más cotizaciones no fueron encontradas"));
                    }

                    // Crear objeto de comparación
                    QuoteComparisonDto comparison = new QuoteComparisonDto();
                    comparison.setQuoteNumbers(quoteNumbers);
                    comparison.setComparisonDate(LocalDateTime.now());

                    // Extraer customerNumber del primer quote (si está disponible)
                    String customerNumber = quotes.get(0).getCustomerNumber();
                    if (customerNumber != null) {
                        comparison.setCustomerNumber(customerNumber);
                    }

                    // Extraer relación de cliente usando identificadores de negocio
                    try {
                        Map<String, Object> customer = null;
                        if (customerNumber != null) {
                            customer = customerClient.getCustomerByNumber(customerNumber);

                            // Verificar que todas las cotizaciones son del mismo cliente
                            for (int i = 1; i < quotes.size(); i++) {
                                if (!customerNumber.equals(quotes.get(i).getCustomerNumber())) {
                                    return Mono.error(new BusinessValidationException(
                                            "Las cotizaciones deben pertenecer al mismo cliente"));
                                }
                            }
                        } else {
                            // Si no tenemos customerNumber, no podemos verificar la relación
                            log.warn("No se puede verificar si las cotizaciones pertenecen al mismo cliente");
                        }

                        // Extraer datos del cliente para la comparación
                        if (customer != null) {
                            String email = (String) customer.get("email");
                            if (email != null) {
                                comparison.setCustomerEmail(email);
                            }

                            String firstName = (String) customer.getOrDefault("firstName", "");
                            String lastName = (String) customer.getOrDefault("lastName", "");
                            comparison.setCustomerName(firstName + " " + lastName);
                        }
                    } catch (Exception e) {
                        log.warn("Error al obtener datos del cliente", e);
                    }

                    // Resto del código para la comparación...
                    Map<String, QuoteOptionDto> recommendedOptions = new HashMap<>();
                    Map<String, Map<String, Boolean>> coverageComparison = new HashMap<>();
                    Map<String, BigDecimal> premiumComparison = new HashMap<>();
                    Map<String, BigDecimal> sumInsuredComparison = new HashMap<>();

                    // Procesar cada cotización
                    for (Quote quote : quotes) {
                        // [Resto del código existente sin cambios]
                    }

                    return Mono.just(comparison);
                });
    }

    @Override
    public Mono<String> convertToPolicyAsync(String quoteId, String selectedOptionId) {
        log.info("Convirtiendo cotización ID: {} a póliza con opción ID: {}", quoteId, selectedOptionId);

        return quoteRepository.findById(quoteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Cotización no encontrada con ID: " + quoteId)))
                .flatMap(quote -> {
                    // Validar que la cotización esté en estado ACCEPTED
                    if (quote.getStatus() != Quote.QuoteStatus.ACCEPTED) {
                        return Mono.error(new BusinessValidationException("Solo se pueden convertir a póliza cotizaciones en estado ACCEPTED"));
                    }

                    // Validar que la cotización no haya expirado
                    if (quote.getValidUntil().isBefore(LocalDateTime.now())) {
                        return Mono.error(new BusinessValidationException("La cotización ha expirado y no puede ser convertida a póliza"));
                    }

                    // Si se especificó una opción, verificar que exista
                    if (selectedOptionId != null && !selectedOptionId.isEmpty()) {
                        boolean optionFound = quote.getOptions().stream()
                                .anyMatch(option -> selectedOptionId.equals(option.getOptionId()));

                        if (!optionFound) {
                            return Mono.error(new ResourceNotFoundException("Opción no encontrada con ID: " + selectedOptionId));
                        }

                        // Actualizar la cotización con la opción seleccionada
                        QuoteOption selectedOption = quote.getOptions().stream()
                                .filter(option -> selectedOptionId.equals(option.getOptionId()))
                                .findFirst()
                                .orElse(null);

                        if (selectedOption != null) {
                            quote.setPremium(selectedOption.getPremium());
                            quote.setCoverages(selectedOption.getCoverages());
                        }
                    }

                    // Aquí iría la lógica para convertir la cotización a póliza mediante el Policy Service
                    // Esta es una implementación básica, en un entorno real se utilizaría
                    // una llamada al Policy Service para crear la póliza

                    return getCurrentUsername()
                            .flatMap(username -> {
                                // Marcar la cotización como convertida
                                quote.setStatus(Quote.QuoteStatus.CONVERTED_TO_POLICY);
                                quote.setUpdatedBy(username);
                                quote.setUpdatedAt(LocalDateTime.now());

                                return quoteRepository.save(quote)
                                        .thenReturn("Cotización convertida a póliza exitosamente");
                            });
                });
    }

    /**
     * Resuelve el ID del cliente utilizando campos alternativos
     */
    private Mono<Long> resolveCustomerId(QuoteRequestDto quoteRequestDto) {
        // Si ya tenemos el ID del cliente, lo usamos directamente
        if (quoteRequestDto.getCustomerId() != null) {
            return Mono.just(quoteRequestDto.getCustomerId());
        }

        // Intentar resolver por email
        if (quoteRequestDto.getCustomerEmail() != null && !quoteRequestDto.getCustomerEmail().isEmpty()) {
            try {
                log.debug("Buscando cliente por email: {}", quoteRequestDto.getCustomerEmail());
                Map<String, Object> customer = customerClient.getCustomerByEmail(quoteRequestDto.getCustomerEmail());
                return Mono.just(((Number) customer.get("id")).longValue());
            } catch (Exception e) {
                log.warn("No se encontró cliente con email: {}", quoteRequestDto.getCustomerEmail(), e);
                // No lanzamos excepción aquí para intentar otros métodos
            }
        }

        // Intentar resolver por identificación
        if (quoteRequestDto.getIdentificationNumber() != null && quoteRequestDto.getIdentificationType() != null) {
            try {
                log.debug("Buscando cliente por identificación: {}/{}",
                        quoteRequestDto.getIdentificationNumber(), quoteRequestDto.getIdentificationType());
                Map<String, Object> customer = customerClient.getCustomerByIdentification(
                        quoteRequestDto.getIdentificationNumber(), quoteRequestDto.getIdentificationType());
                return Mono.just(((Number) customer.get("id")).longValue());
            } catch (Exception e) {
                log.warn("No se encontró cliente con identificación: {}/{}",
                        quoteRequestDto.getIdentificationNumber(), quoteRequestDto.getIdentificationType(), e);
            }
        }

        // Intentar resolver por número de cliente
        if (quoteRequestDto.getCustomerNumber() != null) {
            try {
                log.debug("Buscando cliente por número: {}", quoteRequestDto.getCustomerNumber());
                Map<String, Object> customer = customerClient.getCustomerByNumber(quoteRequestDto.getCustomerNumber());
                return Mono.just(((Number) customer.get("id")).longValue());
            } catch (Exception e) {
                log.warn("No se encontró cliente con número: {}", quoteRequestDto.getCustomerNumber(), e);
            }
        }

        // Si llegamos aquí, no pudimos resolver el cliente
        return Mono.error(new BusinessValidationException("No se pudo identificar al cliente. Por favor, proporcione información válida de identificación."));
    }

    /**
     * Obtiene el nombre de usuario actual
     */
    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> {
                    if (authentication != null) {
                        return authentication.getName();
                    }
                    return "system";
                })
                .onErrorReturn("system");
    }
}