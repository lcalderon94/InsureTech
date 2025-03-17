package com.insurtech.customer.service;

import com.insurtech.customer.model.dto.CustomerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    /**
     * Crea un nuevo cliente
     */
    CustomerDto createCustomer(CustomerDto customerDto);

    /**
     * Obtiene un cliente por su ID
     */
    Optional<CustomerDto> getCustomerById(Long id);

    /**
     * Obtiene un cliente por su número de cliente
     */
    Optional<CustomerDto> getCustomerByNumber(String customerNumber);

    /**
     * Obtiene un cliente por su email
     */
    Optional<CustomerDto> getCustomerByEmail(String email);

    /**
     * Busca clientes por término de búsqueda
     */
    Page<CustomerDto> searchCustomers(String searchTerm, Pageable pageable);

    /**
     * Obtiene todos los clientes paginados
     */
    Page<CustomerDto> getAllCustomers(Pageable pageable);

    /**
     * Actualiza un cliente existente
     */
    CustomerDto updateCustomer(Long id, CustomerDto customerDto);

    /**
     * Elimina un cliente por su ID
     */
    void deleteCustomer(Long id);

    /**
     * Actualiza el estado de un cliente
     */
    CustomerDto updateCustomerStatus(Long id, String status);

    /**
     * Añade un cliente a un segmento
     */
    CustomerDto addCustomerToSegment(Long customerId, Long segmentId);

    /**
     * Elimina un cliente de un segmento
     */
    CustomerDto removeCustomerFromSegment(Long customerId, Long segmentId);

    /**
     * Obtiene todos los clientes de un segmento
     */
    List<CustomerDto> getCustomersBySegment(Long segmentId);

    /**
     * Valida si un email ya está registrado
     */
    boolean isEmailRegistered(String email);

    /**
     * Valida si una identificación ya está registrada
     */
    boolean isIdentificationRegistered(String identificationNumber, String identificationType);
}