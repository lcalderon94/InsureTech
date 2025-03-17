package com.insurtech.customer.model.dto;

import com.insurtech.customer.model.entity.Preference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceDto {

    private Long id;

    @NotNull(message = "El tipo de preferencia es obligatorio")
    private Preference.PreferenceType preferenceType;

    @NotBlank(message = "La clave de preferencia es obligatoria")
    private String preferenceKey;

    private String preferenceValue;
}