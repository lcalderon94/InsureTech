package com.insurtech.customer.model.dto;

import com.insurtech.customer.model.entity.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private Long id;

    private String customerNumber;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "El número de identificación es obligatorio")
    private String identificationNumber;

    private String identificationType;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDateTime dateOfBirth;

    private String gender;

    private Customer.CustomerStatus status;

    private String riskProfile;

    @Valid
    private Set<AddressDto> addresses = new HashSet<>();

    @Valid
    private Set<ContactMethodDto> contactMethods = new HashSet<>();

    @Valid
    private Set<PreferenceDto> preferences = new HashSet<>();

    private Set<Long> segmentIds = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}