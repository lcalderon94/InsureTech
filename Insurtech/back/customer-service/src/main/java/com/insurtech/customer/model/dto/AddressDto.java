package com.insurtech.customer.model.dto;

import com.insurtech.customer.model.entity.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private Long id;

    @NotNull(message = "El tipo de dirección es obligatorio")
    private Address.AddressType addressType;

    @NotBlank(message = "La calle es obligatoria")
    private String street;

    private String number;

    private String unit;

    @NotBlank(message = "La ciudad es obligatoria")
    private String city;

    private String state;

    private String postalCode;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    private boolean isPrimary;

    private boolean isValid;

    private String latitude;

    private String longitude;
}