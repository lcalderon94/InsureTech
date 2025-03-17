package com.insurtech.customer.model.dto;

import com.insurtech.customer.model.entity.ContactMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactMethodDto {

    private Long id;

    @NotNull(message = "El tipo de contacto es obligatorio")
    private ContactMethod.ContactType contactType;

    @NotBlank(message = "El valor de contacto es obligatorio")
    private String contactValue;

    private boolean isPrimary;

    private boolean isVerified;

    private boolean isOptedIn;
}