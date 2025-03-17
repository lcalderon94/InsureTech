package com.insurtech.customer.util;

import com.insurtech.customer.model.dto.AddressDto;
import com.insurtech.customer.model.dto.ContactMethodDto;
import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.model.dto.PreferenceDto;
import com.insurtech.customer.model.entity.Address;
import com.insurtech.customer.model.entity.ContactMethod;
import com.insurtech.customer.model.entity.Customer;
import com.insurtech.customer.model.entity.Preference;
import com.insurtech.customer.model.entity.Segment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Utilidad para mapear entre entidades y DTOs
 */
@Component
public class EntityDtoMapper {

    /**
     * Convierte una entidad Customer a DTO
     */
    public CustomerDto toDto(Customer entity) {
        if (entity == null) return null;

        CustomerDto dto = new CustomerDto();
        dto.setId(entity.getId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setIdentificationNumber(entity.getIdentificationNumber());
        dto.setIdentificationType(entity.getIdentificationType());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setGender(entity.getGender());
        dto.setStatus(entity.getStatus());
        dto.setRiskProfile(entity.getRiskProfile());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Mapear direcciones
        if (entity.getAddresses() != null) {
            dto.setAddresses(entity.getAddresses().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        // Mapear métodos de contacto
        if (entity.getContactMethods() != null) {
            dto.setContactMethods(entity.getContactMethods().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        // Mapear preferencias
        if (entity.getPreferences() != null) {
            dto.setPreferences(entity.getPreferences().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        // Mapear IDs de segmentos
        if (entity.getSegments() != null) {
            dto.setSegmentIds(entity.getSegments().stream()
                    .map(Segment::getId)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    /**
     * Convierte un DTO Customer a entidad
     */
    public Customer toEntity(CustomerDto dto) {
        if (dto == null) return null;

        Customer entity = new Customer();
        entity.setId(dto.getId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setIdentificationNumber(dto.getIdentificationNumber());
        entity.setIdentificationType(dto.getIdentificationType());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setGender(dto.getGender());
        entity.setStatus(dto.getStatus());
        entity.setRiskProfile(dto.getRiskProfile());

        // Inicializar colecciones
        entity.setAddresses(new HashSet<>());
        entity.setContactMethods(new HashSet<>());
        entity.setPreferences(new HashSet<>());
        entity.setSegments(new HashSet<>());

        // Mapear direcciones
        if (dto.getAddresses() != null) {
            dto.getAddresses().forEach(addressDto -> {
                Address address = toEntity(addressDto);
                address.setCustomer(entity);
                entity.getAddresses().add(address);
            });
        }

        // Mapear métodos de contacto
        if (dto.getContactMethods() != null) {
            dto.getContactMethods().forEach(contactMethodDto -> {
                ContactMethod contactMethod = toEntity(contactMethodDto);
                contactMethod.setCustomer(entity);
                entity.getContactMethods().add(contactMethod);
            });
        }

        // Mapear preferencias
        if (dto.getPreferences() != null) {
            dto.getPreferences().forEach(preferenceDto -> {
                Preference preference = toEntity(preferenceDto);
                preference.setCustomer(entity);
                entity.getPreferences().add(preference);
            });
        }

        return entity;
    }

    /**
     * Convierte una entidad Address a DTO
     */
    public AddressDto toDto(Address entity) {
        if (entity == null) return null;

        AddressDto dto = new AddressDto();
        dto.setId(entity.getId());
        dto.setAddressType(entity.getAddressType());
        dto.setStreet(entity.getStreet());
        dto.setNumber(entity.getNumber());
        dto.setUnit(entity.getUnit());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setPrimary(entity.isPrimary());
        dto.setValid(entity.isValid());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());

        return dto;
    }

    /**
     * Convierte un DTO Address a entidad
     */
    public Address toEntity(AddressDto dto) {
        if (dto == null) return null;

        Address entity = new Address();
        entity.setId(dto.getId());
        entity.setAddressType(dto.getAddressType());
        entity.setStreet(dto.getStreet());
        entity.setNumber(dto.getNumber());
        entity.setUnit(dto.getUnit());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountry(dto.getCountry());
        entity.setPrimary(dto.isPrimary());
        entity.setValid(dto.isValid());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());

        return entity;
    }

    /**
     * Convierte una entidad ContactMethod a DTO
     */
    public ContactMethodDto toDto(ContactMethod entity) {
        if (entity == null) return null;

        ContactMethodDto dto = new ContactMethodDto();
        dto.setId(entity.getId());
        dto.setContactType(entity.getContactType());
        dto.setContactValue(entity.getContactValue());
        dto.setPrimary(entity.isPrimary());
        dto.setVerified(entity.isVerified());
        dto.setOptedIn(entity.isOptedIn());

        return dto;
    }

    /**
     * Convierte un DTO ContactMethod a entidad
     */
    public ContactMethod toEntity(ContactMethodDto dto) {
        if (dto == null) return null;

        ContactMethod entity = new ContactMethod();
        entity.setId(dto.getId());
        entity.setContactType(dto.getContactType());
        entity.setContactValue(dto.getContactValue());
        entity.setPrimary(dto.isPrimary());
        entity.setVerified(dto.isVerified());
        entity.setOptedIn(dto.isOptedIn());

        return entity;
    }

    /**
     * Convierte una entidad Preference a DTO
     */
    public PreferenceDto toDto(Preference entity) {
        if (entity == null) return null;

        PreferenceDto dto = new PreferenceDto();
        dto.setId(entity.getId());
        dto.setPreferenceType(entity.getPreferenceType());
        dto.setPreferenceKey(entity.getPreferenceKey());
        dto.setPreferenceValue(entity.getPreferenceValue());

        return dto;
    }

    /**
     * Convierte un DTO Preference a entidad
     */
    public Preference toEntity(PreferenceDto dto) {
        if (dto == null) return null;

        Preference entity = new Preference();
        entity.setId(dto.getId());
        entity.setPreferenceType(dto.getPreferenceType());
        entity.setPreferenceKey(dto.getPreferenceKey());
        entity.setPreferenceValue(dto.getPreferenceValue());

        return entity;
    }
}