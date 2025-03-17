package com.insurtech.customer.model.dto;

import com.insurtech.customer.model.entity.Segment;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDto {

    private Long id;

    @NotBlank(message = "El nombre del segmento es obligatorio")
    private String name;

    private String description;

    private Segment.SegmentType segmentType;

    private String segmentCriteria;

    private boolean isActive = true;
}