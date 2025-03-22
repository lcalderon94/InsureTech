package com.insurtech.payment.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Component
public class MoneyCalculator {

    private static final Map<String, Currency> CURRENCY_CACHE = new HashMap<>();
    private static final int DEFAULT_SCALE = 2;

    /**
     * Redondea un importe según la moneda
     */
    public BigDecimal roundAmount(BigDecimal amount, String currencyCode) {
        Currency currency = getCurrency(currencyCode);
        int scale = currency.getDefaultFractionDigits();
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el importe con impuestos
     */
    public BigDecimal calculateWithTax(BigDecimal amount, BigDecimal taxRate, String currencyCode) {
        BigDecimal taxAmount = amount.multiply(taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return roundAmount(amount.add(taxAmount), currencyCode);
    }

    /**
     * Calcula el importe de un impuesto
     */
    public BigDecimal calculateTaxAmount(BigDecimal amount, BigDecimal taxRate, String currencyCode) {
        BigDecimal taxAmount = amount.multiply(taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return roundAmount(taxAmount, currencyCode);
    }

    /**
     * Calcula el porcentaje de un importe
     */
    public BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage, String currencyCode) {
        BigDecimal result = amount.multiply(percentage.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return roundAmount(result, currencyCode);
    }

    /**
     * Comprueba si dos importes son iguales (teniendo en cuenta la escala de la moneda)
     */
    public boolean areEqual(BigDecimal amount1, BigDecimal amount2, String currencyCode) {
        Currency currency = getCurrency(currencyCode);
        int scale = currency.getDefaultFractionDigits();

        BigDecimal scaledAmount1 = amount1.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal scaledAmount2 = amount2.setScale(scale, RoundingMode.HALF_UP);

        return scaledAmount1.compareTo(scaledAmount2) == 0;
    }

    /**
     * Obtiene una instancia de Currency a partir del código ISO
     */
    private Currency getCurrency(String currencyCode) {
        return CURRENCY_CACHE.computeIfAbsent(currencyCode, Currency::getInstance);
    }
}