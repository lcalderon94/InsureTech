package com.insurtech.customer.service.impl;

import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.model.entity.Customer;
import com.insurtech.customer.model.entity.Segment;
import com.insurtech.customer.repository.CustomerRepository;
import com.insurtech.customer.repository.SegmentRepository;
import com.insurtech.customer.service.CustomerBatchService;
import com.insurtech.customer.service.CustomerService;
import com.insurtech.customer.util.EntityDtoMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class CustomerBatchServiceImpl implements CustomerBatchService {

    private static final Logger log = LoggerFactory.getLogger(CustomerBatchServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final SegmentRepository segmentRepository;
    private final CustomerService customerService;
    private final EntityDtoMapper mapper;

    @Autowired
    public CustomerBatchServiceImpl(
            CustomerRepository customerRepository,
            SegmentRepository segmentRepository,
            CustomerService customerService,
            EntityDtoMapper mapper) {
        this.customerRepository = customerRepository;
        this.segmentRepository = segmentRepository;
        this.customerService = customerService;
        this.mapper = mapper;
    }

    @Async
    @Override
    @Transactional
    public Future<List<CustomerDto>> processBatch(List<CustomerDto> customers) {
        log.info("Processing batch of {} customers", customers.size());

        List<CustomerDto> processedCustomers = new ArrayList<>();

        for (CustomerDto customerDto : customers) {
            try {
                CustomerDto processedCustomer = customerService.createCustomer(customerDto);
                processedCustomers.add(processedCustomer);
                log.debug("Processed customer: {}", processedCustomer.getId());
            } catch (Exception e) {
                log.error("Error processing customer: {}", customerDto, e);
                // Continuar con el siguiente cliente en caso de error
            }
        }

        log.info("Batch processing completed. Processed {}/{} customers successfully",
                processedCustomers.size(), customers.size());

        return new AsyncResult<>(processedCustomers);
    }

    @Async
    @Override
    @Transactional
    public Future<List<CustomerDto>> processCustomersFromCsv(InputStream inputStream) {
        log.info("Processing customers from CSV");

        List<CustomerDto> customers = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                try {
                    CustomerDto customer = new CustomerDto();
                    customer.setFirstName(csvRecord.get("firstName"));
                    customer.setLastName(csvRecord.get("lastName"));
                    customer.setEmail(csvRecord.get("email"));
                    customer.setIdentificationNumber(csvRecord.get("identificationNumber"));
                    customer.setIdentificationType(csvRecord.get("identificationType"));

                    // Añadir más campos según la estructura del CSV

                    customers.add(customer);
                } catch (Exception e) {
                    log.error("Error parsing CSV record: {}", csvRecord, e);
                    // Continuar con el siguiente registro en caso de error
                }
            }

            log.info("CSV parsing completed. Found {} customers", customers.size());

            // Procesar los clientes encontrados
            return processBatch(customers);

        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            return new AsyncResult<>(Collections.emptyList());
        }
    }

    @Async
    @Override
    @Transactional
    public Future<Integer> updateCustomersBySegment(Long segmentId, String fieldName, String fieldValue) {
        log.info("Updating field '{}' to '{}' for customers in segment {}", fieldName, fieldValue, segmentId);

        // Verificar que el segmento existe
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new NoSuchElementException("Segmento no encontrado con ID: " + segmentId));

        List<Customer> customers = customerRepository.findBySegmentId(segmentId);
        log.info("Found {} customers in segment {}", customers.size(), segmentId);

        int updatedCount = 0;

        for (Customer customer : customers) {
            try {
                // Intentar actualizar el campo mediante reflexión
                Field field = Customer.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(customer, fieldValue);

                customer.setUpdatedBy("batch-system");
                customer.setUpdatedAt(LocalDateTime.now());

                customerRepository.save(customer);
                updatedCount++;

            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("Error updating field '{}' for customer {}", fieldName, customer.getId(), e);
                // Continuar con el siguiente cliente en caso de error
            }
        }

        log.info("Batch update completed. Updated {}/{} customers successfully",
                updatedCount, customers.size());

        return new AsyncResult<>(updatedCount);
    }

    @Async
    @Override
    @Transactional(readOnly = true)
    public Future<Map<String, Object>> calculateSegmentStatistics(Long segmentId) {
        log.info("Calculating statistics for segment {}", segmentId);

        Map<String, Object> statistics = new HashMap<>();

        // Verificar que el segmento existe
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new NoSuchElementException("Segmento no encontrado con ID: " + segmentId));

        List<Customer> customers = customerRepository.findBySegmentId(segmentId);
        log.info("Found {} customers in segment {}", customers.size(), segmentId);

        // Estadísticas básicas
        statistics.put("totalCustomers", customers.size());
        statistics.put("segmentName", segment.getName());

        // Distribución por género
        Map<String, Long> genderDistribution = customers.stream()
                .filter(c -> c.getGender() != null)
                .collect(Collectors.groupingBy(Customer::getGender, Collectors.counting()));
        statistics.put("genderDistribution", genderDistribution);

        // Distribución por estado
        Map<Customer.CustomerStatus, Long> statusDistribution = customers.stream()
                .collect(Collectors.groupingBy(Customer::getStatus, Collectors.counting()));
        statistics.put("statusDistribution", statusDistribution);

        // Ciudades más comunes
        Map<String, Long> cityDistribution = customers.stream()
                .flatMap(c -> c.getAddresses().stream())
                .filter(a -> a.getCity() != null)
                .collect(Collectors.groupingBy(a -> a.getCity(), Collectors.counting()));
        statistics.put("topCities", cityDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        log.info("Statistics calculation completed for segment {}", segmentId);

        return new AsyncResult<>(statistics);
    }

    @Async
    @Override
    @Transactional
    public Future<List<Long>> validateAddressesBatch(List<Long> customerIds) {
        log.info("Validating addresses for {} customers", customerIds.size());

        List<Long> validatedCustomers = new ArrayList<>();

        for (Long customerId : customerIds) {
            try {
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new NoSuchElementException("Cliente no encontrado con ID: " + customerId));

                // Simular validación de direcciones
                // En un caso real, aquí se llamaría a un servicio de validación externo
                for (var address : customer.getAddresses()) {
                    if (address.getStreet() != null && address.getCity() != null && address.getCountry() != null) {
                        address.setValid(true);
                    } else {
                        address.setValid(false);
                    }
                    address.setUpdatedBy("address-validator");
                    address.setUpdatedAt(LocalDateTime.now());
                }

                customerRepository.save(customer);
                validatedCustomers.add(customerId);

                log.debug("Validated addresses for customer {}", customerId);

            } catch (Exception e) {
                log.error("Error validating addresses for customer {}", customerId, e);
                // Continuar con el siguiente cliente en caso de error
            }
        }

        log.info("Address validation completed. Validated {}/{} customers successfully",
                validatedCustomers.size(), customerIds.size());

        return new AsyncResult<>(validatedCustomers);
    }

    @Async
    @Override
    @Transactional(readOnly = true)
    public Future<byte[]> generateCustomerReport(String reportType, List<Long> customerIds) {
        log.info("Generating {} report for {} customers", reportType, customerIds.size());

        // Obtener los clientes
        List<Customer> customers = customerRepository.findAllById(customerIds);
        log.info("Found {} customers out of {} requested", customers.size(), customerIds.size());

        // Simulación de generación de informe
        // En un caso real, aquí se utilizaría una biblioteca como JasperReports

        StringBuilder reportContent = new StringBuilder();
        reportContent.append("INFORME DE CLIENTES\n");
        reportContent.append("Tipo: ").append(reportType).append("\n");
        reportContent.append("Fecha: ").append(LocalDateTime.now()).append("\n\n");

        reportContent.append("ID | Nombre | Apellido | Email | Estado\n");
        reportContent.append("------------------------------------------\n");

        for (Customer customer : customers) {
            reportContent.append(customer.getId()).append(" | ")
                    .append(customer.getFirstName()).append(" | ")
                    .append(customer.getLastName()).append(" | ")
                    .append(customer.getEmail()).append(" | ")
                    .append(customer.getStatus()).append("\n");
        }

        log.info("Report generation completed");

        return new AsyncResult<>(reportContent.toString().getBytes());
    }
}