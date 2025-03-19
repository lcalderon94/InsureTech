package com.insurtech.quote.service.impl;

import com.insurtech.quote.client.CustomerClient;
import com.insurtech.quote.client.PolicyClient;
import com.insurtech.quote.model.dto.QuoteCoverageDto;
import com.insurtech.quote.model.dto.QuoteDto;
import com.insurtech.quote.model.dto.QuoteOptionDto;
import com.insurtech.quote.model.dto.QuoteRequestDto;
import com.insurtech.quote.model.entity.Quote;
import com.insurtech.quote.model.entity.QuoteCoverage;
import com.insurtech.quote.service.QuoteCalculationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class QuoteCalculationServiceImpl implements QuoteCalculationService {

    private static final Logger log = LoggerFactory.getLogger(QuoteCalculationServiceImpl.class);

    private final CustomerClient customerClient;
    private final PolicyClient policyClient;

    @Override
    public Mono<BigDecimal> calculateQuotePremium(QuoteDto quoteDto) {
        log.info("Calculando prima para cotización tipo: {}", quoteDto.getQuoteType());

        // Base de cálculo dependiendo del tipo de cotización
        double baseRate = getBaseRate(quoteDto.getQuoteType());

        // Cálculos basados en la suma asegurada
        BigDecimal sumInsured = quoteDto.getSumInsured();
        if (sumInsured == null) {
            sumInsured = BigDecimal.ZERO;
        }

        BigDecimal basePremium = sumInsured.multiply(BigDecimal.valueOf(baseRate))
                .setScale(2, RoundingMode.HALF_UP);

        // Ajustes por coberturas
        if (quoteDto.getCoverages() != null && !quoteDto.getCoverages().isEmpty()) {
            BigDecimal coveragesPremium = quoteDto.getCoverages().stream()
                    .filter(c -> c.getPremium() != null)
                    .map(QuoteCoverageDto::getPremium)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Si no hay primas específicas por cobertura, calculamos en base a la cantidad
            if (coveragesPremium.compareTo(BigDecimal.ZERO) == 0) {
                basePremium = basePremium.multiply(BigDecimal.valueOf(1 + 0.1 * quoteDto.getCoverages().size()))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                basePremium = basePremium.add(coveragesPremium);
            }
        }

        // Aplicar descuentos
        return applyDiscounts(basePremium, quoteDto.getCustomerId(), null)
                .defaultIfEmpty(basePremium);
    }

    @Override
    public Mono<BigDecimal> calculateRequestPremium(QuoteRequestDto requestDto) {
        log.info("Calculando prima preliminar para solicitud tipo: {}", requestDto.getQuoteType());

        // Base de cálculo dependiendo del tipo de cotización
        double baseRate = getBaseRate(requestDto.getQuoteType());

        // Cálculos basados en la suma asegurada
        BigDecimal sumInsured = requestDto.getSumInsured();
        if (sumInsured == null) {
            sumInsured = BigDecimal.ZERO;
        }

        BigDecimal basePremium = sumInsured.multiply(BigDecimal.valueOf(baseRate))
                .setScale(2, RoundingMode.HALF_UP);

        // Aplicar descuentos
        return applyDiscounts(basePremium, requestDto.getCustomerId(), requestDto)
                .defaultIfEmpty(basePremium);
    }

    @Override
    public Mono<BigDecimal> calculateCoveragePremium(QuoteCoverageDto coverageDto, Quote.QuoteType quoteType) {
        log.info("Calculando prima para cobertura: {} en tipo: {}", coverageDto.getCoverageCode(), quoteType);

        // Si ya tiene prima definida, usarla
        if (coverageDto.getPremium() != null) {
            return Mono.just(coverageDto.getPremium());
        }

        // Si tiene tasa y suma asegurada, calcular
        if (coverageDto.getRate() != null && coverageDto.getSumInsured() != null) {
            return Mono.just(coverageDto.getSumInsured()
                    .multiply(coverageDto.getRate())
                    .setScale(2, RoundingMode.HALF_UP));
        }

        // Cálculo genérico basado en tipo de seguro y cobertura
        double baseCoverageRate = 0.01; // 1% como base

        // Ajustar tasa según tipo de seguro
        switch (quoteType) {
            case AUTO:
                baseCoverageRate = 0.02; // 2%
                break;
            case HOME:
                baseCoverageRate = 0.015; // 1.5%
                break;
            case LIFE:
                baseCoverageRate = 0.03; // 3%
                break;
            case HEALTH:
                baseCoverageRate = 0.04; // 4%
                break;
            default:
                baseCoverageRate = 0.025; // 2.5%
        }

        // Calcular prima
        BigDecimal premium = coverageDto.getSumInsured()
                .multiply(BigDecimal.valueOf(baseCoverageRate))
                .setScale(2, RoundingMode.HALF_UP);

        return Mono.just(premium);
    }

    @Override
    public Mono<Set<QuoteOptionDto>> generateQuoteOptions(QuoteRequestDto requestDto) {
        log.info("Generando opciones para cotización tipo: {}", requestDto.getQuoteType());

        // Crear conjunto para almacenar opciones
        Set<QuoteOptionDto> options = new HashSet<>();

        // Generar opción básica
        QuoteOptionDto basicOption = createBasicOption(requestDto);
        options.add(basicOption);

        // Generar opción estándar
        QuoteOptionDto standardOption = createStandardOption(requestDto);
        standardOption.setRecommended(true); // Marcar como recomendada
        options.add(standardOption);

        // Generar opción premium
        QuoteOptionDto premiumOption = createPremiumOption(requestDto);
        options.add(premiumOption);

        return Mono.just(options);
    }

    @Override
    public Mono<BigDecimal> applyDiscounts(BigDecimal basePremium, Long customerId, QuoteRequestDto requestDto) {
        log.info("Aplicando descuentos para cliente ID: {}", customerId);

        // Si no hay cliente identificado, no aplicar descuentos
        if (customerId == null) {
            return Mono.just(basePremium);
        }

        try {
            // Obtener pólizas existentes del cliente para descuento por cartera
            List<Map<String, Object>> customerPolicies = policyClient.getPoliciesByCustomerId(customerId);

            // Descuento por cliente existente (más de una póliza)
            double discountFactor = 1.0;

            if (customerPolicies != null && customerPolicies.size() > 0) {
                // Descuento progresivo según número de pólizas (hasta 15%)
                discountFactor -= Math.min(customerPolicies.size() * 0.03, 0.15);
            }

            // Aplicar descuento
            return Mono.just(basePremium.multiply(BigDecimal.valueOf(discountFactor))
                    .setScale(2, RoundingMode.HALF_UP));

        } catch (Exception e) {
            log.warn("Error al verificar pólizas para descuentos", e);
            return Mono.just(basePremium);
        }
    }

    // Métodos auxiliares

    private double getBaseRate(Quote.QuoteType quoteType) {
        switch (quoteType) {
            case AUTO:
                return 0.05; // 5%
            case HOME:
                return 0.03; // 3%
            case LIFE:
                return 0.07; // 7%
            case HEALTH:
                return 0.08; // 8%
            case TRAVEL:
                return 0.04; // 4%
            case BUSINESS:
                return 0.06; // 6%
            default:
                return 0.05; // 5%
        }
    }

    private QuoteOptionDto createBasicOption(QuoteRequestDto requestDto) {
        QuoteOptionDto option = new QuoteOptionDto();
        option.setOptionId(UUID.randomUUID().toString());
        option.setName("Básico");
        option.setDescription("Cobertura básica con protección esencial");
        option.setDisplayOrder(1);

        // Calcular prima con 10% menos que la estándar
        BigDecimal sumInsured = requestDto.getSumInsured();
        double baseRate = getBaseRate(requestDto.getQuoteType()) * 0.9; // 10% menos

        option.setPremium(sumInsured.multiply(BigDecimal.valueOf(baseRate))
                .setScale(2, RoundingMode.HALF_UP));
        option.setSumInsured(sumInsured);

        // Añadir coberturas básicas según tipo
        Set<QuoteCoverageDto> coverages = new HashSet<>();

        // Añadir coberturas según tipo de seguro
        switch (requestDto.getQuoteType()) {
            case AUTO:
                coverages.add(createCoverage("RC", "Responsabilidad Civil", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.6))));
                coverages.add(createCoverage("DA", "Daños Materiales", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.3))));
                break;
            case HOME:
                coverages.add(createCoverage("INC", "Incendio", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.7))));
                coverages.add(createCoverage("ROB", "Robo", sumInsured.multiply(BigDecimal.valueOf(0.3)), option.getPremium().multiply(BigDecimal.valueOf(0.2))));
                break;
            case LIFE:
                coverages.add(createCoverage("FAL", "Fallecimiento", sumInsured, option.getPremium()));
                break;
            default:
                coverages.add(createCoverage("BAS", "Cobertura Básica", sumInsured, option.getPremium()));
        }

        option.setCoverages(coverages);

        return option;
    }

    private QuoteOptionDto createStandardOption(QuoteRequestDto requestDto) {
        QuoteOptionDto option = new QuoteOptionDto();
        option.setOptionId(UUID.randomUUID().toString());
        option.setName("Estándar");
        option.setDescription("Cobertura estándar con buena relación calidad-precio");
        option.setDisplayOrder(2);

        // Calcular prima estándar
        BigDecimal sumInsured = requestDto.getSumInsured();
        double baseRate = getBaseRate(requestDto.getQuoteType());

        option.setPremium(sumInsured.multiply(BigDecimal.valueOf(baseRate))
                .setScale(2, RoundingMode.HALF_UP));
        option.setSumInsured(sumInsured);

        // Añadir coberturas estándar según tipo
        Set<QuoteCoverageDto> coverages = new HashSet<>();

        // Añadir coberturas según tipo de seguro
        switch (requestDto.getQuoteType()) {
            case AUTO:
                coverages.add(createCoverage("RC", "Responsabilidad Civil", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.5))));
                coverages.add(createCoverage("DA", "Daños Materiales", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.3))));
                coverages.add(createCoverage("RO", "Robo Total", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.2))));
                break;
            case HOME:
                coverages.add(createCoverage("INC", "Incendio", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.5))));
                coverages.add(createCoverage("ROB", "Robo", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.2))));
                coverages.add(createCoverage("DAG", "Daños por Agua", sumInsured.multiply(BigDecimal.valueOf(0.3)), option.getPremium().multiply(BigDecimal.valueOf(0.15))));
                coverages.add(createCoverage("RC", "Responsabilidad Civil", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.15))));
                break;
            case LIFE:
                coverages.add(createCoverage("FAL", "Fallecimiento", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.7))));
                coverages.add(createCoverage("INV", "Invalidez", sumInsured.multiply(BigDecimal.valueOf(0.7)), option.getPremium().multiply(BigDecimal.valueOf(0.3))));
                break;
            default:
                coverages.add(createCoverage("EST", "Cobertura Estándar", sumInsured, option.getPremium()));
        }

        option.setCoverages(coverages);

        return option;
    }

    private QuoteOptionDto createPremiumOption(QuoteRequestDto requestDto) {
        QuoteOptionDto option = new QuoteOptionDto();
        option.setOptionId(UUID.randomUUID().toString());
        option.setName("Premium");
        option.setDescription("Cobertura completa con máxima protección");
        option.setDisplayOrder(3);

        // Calcular prima con 30% más que la estándar
        BigDecimal sumInsured = requestDto.getSumInsured();
        double baseRate = getBaseRate(requestDto.getQuoteType()) * 1.3; // 30% más

        option.setPremium(sumInsured.multiply(BigDecimal.valueOf(baseRate))
                .setScale(2, RoundingMode.HALF_UP));
        option.setSumInsured(sumInsured);

        // Añadir coberturas premium según tipo
        Set<QuoteCoverageDto> coverages = new HashSet<>();

        // Añadir coberturas según tipo de seguro
        switch (requestDto.getQuoteType()) {
            case AUTO:
                coverages.add(createCoverage("RC", "Responsabilidad Civil", sumInsured.multiply(BigDecimal.valueOf(1.5)), option.getPremium().multiply(BigDecimal.valueOf(0.4))));
                coverages.add(createCoverage("DA", "Daños Materiales", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.2))));
                coverages.add(createCoverage("RO", "Robo Total", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.15))));
                coverages.add(createCoverage("RP", "Robo Parcial", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.1))));
                coverages.add(createCoverage("CR", "Cristales", sumInsured.multiply(BigDecimal.valueOf(0.1)), option.getPremium().multiply(BigDecimal.valueOf(0.05))));
                coverages.add(createCoverage("AS", "Asistencia en Viaje", sumInsured.multiply(BigDecimal.valueOf(0.2)), option.getPremium().multiply(BigDecimal.valueOf(0.1))));
                break;
            case HOME:
                coverages.add(createCoverage("INC", "Incendio", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.4))));
                coverages.add(createCoverage("ROB", "Robo", sumInsured.multiply(BigDecimal.valueOf(0.7)), option.getPremium().multiply(BigDecimal.valueOf(0.2))));
                coverages.add(createCoverage("DAG", "Daños por Agua", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.1))));
                coverages.add(createCoverage("RC", "Responsabilidad Civil", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.15))));
                coverages.add(createCoverage("AJ", "Asistencia Jurídica", sumInsured.multiply(BigDecimal.valueOf(0.3)), option.getPremium().multiply(BigDecimal.valueOf(0.05))));
                coverages.add(createCoverage("SAH", "Servicio de Asistencia Hogar", sumInsured.multiply(BigDecimal.valueOf(0.2)), option.getPremium().multiply(BigDecimal.valueOf(0.1))));
                break;
            case LIFE:
                coverages.add(createCoverage("FAL", "Fallecimiento", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.5))));
                coverages.add(createCoverage("INV", "Invalidez", sumInsured, option.getPremium().multiply(BigDecimal.valueOf(0.25))));
                coverages.add(createCoverage("ENF", "Enfermedades Graves", sumInsured.multiply(BigDecimal.valueOf(0.5)), option.getPremium().multiply(BigDecimal.valueOf(0.15))));
                coverages.add(createCoverage("HOS", "Hospitalización", sumInsured.multiply(BigDecimal.valueOf(0.3)), option.getPremium().multiply(BigDecimal.valueOf(0.1))));
                break;
            default:
                coverages.add(createCoverage("PRE", "Cobertura Premium", sumInsured, option.getPremium()));
        }

        option.setCoverages(coverages);

        return option;
    }

    private QuoteCoverageDto createCoverage(String code, String name, BigDecimal sumInsured, BigDecimal premium) {
        QuoteCoverageDto coverage = new QuoteCoverageDto();
        coverage.setCoverageId(UUID.randomUUID().toString());
        coverage.setCoverageCode(code);
        coverage.setCoverageName(name);
        coverage.setSumInsured(sumInsured);
        coverage.setPremium(premium);
        return coverage;
    }
}