package com.insurtech.payment.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentNumberGenerator {

    private static final String PREFIX = "PAY";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);
    private static final Random RANDOM = new Random();

    /**
     * Genera un número de pago único con el formato:
     * PAY-[YYYYMMDD]-[SEQ]-[RANDOM]
     * Donde:
     * - YYYYMMDD es la fecha actual
     * - SEQ es un número secuencial
     * - RANDOM es un número aleatorio de 3 dígitos
     */
    public String generatePaymentNumber() {
        LocalDateTime now = LocalDateTime.now();
        String dateComponent = now.format(DATE_FORMAT);

        // Reiniciar la secuencia cada día
        if (SEQUENCE.get() > 9999) {
            SEQUENCE.set(1);
        }

        String sequenceComponent = String.format("%04d", SEQUENCE.getAndIncrement());
        String randomComponent = String.format("%03d", RANDOM.nextInt(1000));

        return PREFIX + "-" + dateComponent + "-" + sequenceComponent + "-" + randomComponent;
    }

    /**
     * Genera un número de referencia para un tipo específico
     */
    public String generateReferenceNumber(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        String dateComponent = now.format(DATE_FORMAT);
        String sequenceComponent = String.format("%04d", SEQUENCE.getAndIncrement());
        String randomComponent = String.format("%03d", RANDOM.nextInt(1000));

        return prefix + "-" + dateComponent + "-" + sequenceComponent + "-" + randomComponent;
    }

    /**
     * Genera un número de factura único
     */
    public String generateInvoiceNumber() {
        LocalDateTime now = LocalDateTime.now();
        String dateComponent = now.format(DATE_FORMAT);
        String sequenceComponent = String.format("%04d", SEQUENCE.getAndIncrement());
        String randomComponent = String.format("%03d", RANDOM.nextInt(1000));

        return "INV-" + dateComponent + "-" + sequenceComponent + "-" + randomComponent;
    }

    /**
     * Genera un número de método de pago único
     */
    public String generatePaymentMethodNumber() {
        LocalDateTime now = LocalDateTime.now();
        String dateComponent = now.format(DATE_FORMAT);
        String sequenceComponent = String.format("%04d", SEQUENCE.getAndIncrement());
        String randomComponent = String.format("%03d", RANDOM.nextInt(1000));

        return "PMT-" + dateComponent + "-" + sequenceComponent + "-" + randomComponent;
    }

    /**
     * Genera un número de reembolso único
     */
    public String generateRefundNumber() {
        LocalDateTime now = LocalDateTime.now();
        String dateComponent = now.format(DATE_FORMAT);
        String sequenceComponent = String.format("%04d", SEQUENCE.getAndIncrement());
        String randomComponent = String.format("%03d", RANDOM.nextInt(1000));

        return "REF-" + dateComponent + "-" + sequenceComponent + "-" + randomComponent;
    }
}