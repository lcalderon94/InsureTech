package com.insurtech.customer.service.impl;

import com.insurtech.customer.event.producer.CustomerEventProducer;
import com.insurtech.customer.exception.CustomerNotFoundException;
import com.insurtech.customer.exception.ResourceNotFoundException;
import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.model.entity.Customer;
import com.insurtech.customer.model.entity.Segment;
import com.insurtech.customer.repository.CustomerRepository;
import com.insurtech.customer.repository.SegmentRepository;
import com.insurtech.customer.service.CustomerService;
import com.insurtech.customer.util.EntityDtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final SegmentRepository segmentRepository;
    private final EntityDtoMapper mapper;
    private final CustomerEventProducer eventProducer;

    @Autowired
    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            SegmentRepository segmentRepository,
            EntityDtoMapper mapper,
            CustomerEventProducer eventProducer) {
        this.customerRepository = customerRepository;
        this.segmentRepository = segmentRepository;
        this.mapper = mapper;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) {
        log.info("Creating customer: {}", customerDto.getEmail());

        // Validar si el email ya existe
        if (isEmailRegistered(customerDto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + customerDto.getEmail());
        }

        // Validar si la identificación ya existe
        if (isIdentificationRegistered(customerDto.getIdentificationNumber(), customerDto.getIdentificationType())) {
            throw new IllegalArgumentException("La identificación ya está registrada");
        }

        // Generar número de cliente único
        if (customerDto.getCustomerNumber() == null) {
            customerDto.setCustomerNumber(generateCustomerNumber());
        }

        // Crear entidad Customer
        Customer customer = mapper.toEntity(customerDto);
        customer.setCreatedBy(getCurrentUsername());
        customer.setUpdatedBy(getCurrentUsername());

        // Por defecto, el estado es ACTIVE si no se especifica
        if (customer.getStatus() == null) {
            customer.setStatus(Customer.CustomerStatus.ACTIVE);
        }

        // Procesar segmentos
        if (customerDto.getSegmentIds() != null && !customerDto.getSegmentIds().isEmpty()) {
            Set<Segment> segments = new HashSet<>();
            for (Long segmentId : customerDto.getSegmentIds()) {
                Segment segment = segmentRepository.findById(segmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con id: " + segmentId));
                segments.add(segment);
            }
            customer.setSegments(segments);
        }

        // Guardar el cliente
        customer = customerRepository.save(customer);

        // Publicar evento de cliente creado
        //eventProducer.publishCustomerCreated(customer);

        log.info("Customer created successfully with ID: {}", customer.getId());

        return mapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDto> getCustomerById(Long id) {
        log.debug("Getting customer by ID: {}", id);
        return customerRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDto> getCustomerByNumber(String customerNumber) {
        log.debug("Getting customer by number: {}", customerNumber);
        return customerRepository.findByCustomerNumber(customerNumber)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDto> getCustomerByEmail(String email) {
        log.debug("Getting customer by email: {}", email);
        return customerRepository.findByEmail(email)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDto> searchCustomers(String searchTerm, Pageable pageable) {
        log.debug("Searching customers with term: {}", searchTerm);
        return customerRepository.search(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllCustomers(Pageable pageable) {
        log.debug("Getting all customers with pagination");
        return customerRepository.findAll(pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {
        log.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + id));

        // Backup del estado anterior para posible evento de cambio de estado
        Customer.CustomerStatus oldStatus = customer.getStatus();

        // Validar que el email no esté registrado por otro cliente
        if (customerDto.getEmail() != null &&
                !customer.getEmail().equals(customerDto.getEmail()) &&
                isEmailRegistered(customerDto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + customerDto.getEmail());
        }

        // Actualizar campos básicos
        if (customerDto.getFirstName() != null) customer.setFirstName(customerDto.getFirstName());
        if (customerDto.getLastName() != null) customer.setLastName(customerDto.getLastName());
        if (customerDto.getEmail() != null) customer.setEmail(customerDto.getEmail());
        if (customerDto.getDateOfBirth() != null) customer.setDateOfBirth(customerDto.getDateOfBirth());
        if (customerDto.getGender() != null) customer.setGender(customerDto.getGender());
        if (customerDto.getStatus() != null) customer.setStatus(customerDto.getStatus());
        if (customerDto.getRiskProfile() != null) customer.setRiskProfile(customerDto.getRiskProfile());

        customer.setUpdatedBy(getCurrentUsername());

        // Actualizar entidad principal
        customer = customerRepository.save(customer);

        // Publicar evento de cliente actualizado
        eventProducer.publishCustomerUpdated(customer);

        // Publicar evento de cambio de estado si aplica
        if (oldStatus != customer.getStatus()) {
            eventProducer.publishCustomerStatusChanged(customer, oldStatus);
        }

        log.info("Customer updated successfully with ID: {}", id);

        return mapper.toDto(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + id));

        customerRepository.delete(customer);

        log.info("Customer deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public CustomerDto updateCustomerStatus(Long id, String status) {
        log.info("Updating status to {} for customer with ID: {}", status, id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + id));

        try {
            Customer.CustomerStatus oldStatus = customer.getStatus();
            Customer.CustomerStatus customerStatus = Customer.CustomerStatus.valueOf(status);
            customer.setStatus(customerStatus);
            customer.setUpdatedBy(getCurrentUsername());

            customer = customerRepository.save(customer);

            // Publicar evento de cambio de estado
            eventProducer.publishCustomerStatusChanged(customer, oldStatus);

            log.info("Customer status updated successfully with ID: {}", id);

            return mapper.toDto(customer);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + status);
        }
    }

    @Override
    @Transactional
    public CustomerDto addCustomerToSegment(Long customerId, Long segmentId) {
        log.info("Adding customer ID: {} to segment ID: {}", customerId, segmentId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + customerId));

        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + segmentId));

        customer.getSegments().add(segment);
        customer.setUpdatedBy(getCurrentUsername());

        customer = customerRepository.save(customer);

        log.info("Customer added to segment successfully");

        return mapper.toDto(customer);
    }

    @Override
    @Transactional
    public CustomerDto removeCustomerFromSegment(Long customerId, Long segmentId) {
        log.info("Removing customer ID: {} from segment ID: {}", customerId, segmentId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente no encontrado con ID: " + customerId));

        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + segmentId));

        customer.getSegments().remove(segment);
        customer.setUpdatedBy(getCurrentUsername());

        customer = customerRepository.save(customer);

        log.info("Customer removed from segment successfully");

        return mapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersBySegment(Long segmentId) {
        log.debug("Getting customers by segment ID: {}", segmentId);

        // Verificar que el segmento existe
        if (!segmentRepository.existsById(segmentId)) {
            throw new ResourceNotFoundException("Segmento no encontrado con ID: " + segmentId);
        }

        return customerRepository.findBySegmentId(segmentId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIdentificationRegistered(String identificationNumber, String identificationType) {
        return customerRepository.existsByIdentificationNumberAndIdentificationType(
                identificationNumber, identificationType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDto> getCustomerByIdentification(String identificationNumber, String identificationType) {
        log.debug("Getting customer by identification: {}/{}", identificationNumber, identificationType);
        return customerRepository.findByIdentificationNumberAndIdentificationType(identificationNumber, identificationType)
                .map(mapper::toDto);
    }

    // Métodos auxiliares

    private String generateCustomerNumber() {
        // Formato: CUS-YYYYMMDD-XXXX donde XXXX es un número aleatorio
        String datePart = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "CUS-" + datePart + "-" + randomPart;
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}