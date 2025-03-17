package com.insurtech.customer.service;

import com.insurtech.customer.model.dto.CustomerDto;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface CustomerBatchService {

    /**
     * Procesa por lotes una lista de clientes
     */
    Future<List<CustomerDto>> processBatch(List<CustomerDto> customers);

    /**
     * Procesa por lotes un archivo CSV de clientes
     */
    Future<List<CustomerDto>> processCustomersFromCsv(InputStream inputStream);

    /**
     * Realiza una operación de actualización masiva en clientes por segmento
     */
    Future<Integer> updateCustomersBySegment(Long segmentId, String fieldName, String fieldValue);

    /**
     * Calcula estadísticas en un segmento de clientes
     */
    Future<Map<String, Object>> calculateSegmentStatistics(Long segmentId);

    /**
     * Verifica direcciones de forma masiva
     */
    Future<List<Long>> validateAddressesBatch(List<Long> customerIds);

    /**
     * Genera informes a partir de datos de clientes
     */
    Future<byte[]> generateCustomerReport(String reportType, List<Long> customerIds);
}