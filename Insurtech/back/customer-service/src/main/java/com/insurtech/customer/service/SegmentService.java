package com.insurtech.customer.service;

import com.insurtech.customer.model.dto.SegmentDto;

import java.util.List;
import java.util.Optional;

public interface SegmentService {

    /**
     * Crea un nuevo segmento
     */
    SegmentDto createSegment(SegmentDto segmentDto);

    /**
     * Obtiene un segmento por su ID
     */
    Optional<SegmentDto> getSegmentById(Long id);

    /**
     * Obtiene un segmento por su nombre
     */
    Optional<SegmentDto> getSegmentByName(String name);

    /**
     * Obtiene todos los segmentos
     */
    List<SegmentDto> getAllSegments();

    /**
     * Obtiene todos los segmentos activos
     */
    List<SegmentDto> getAllActiveSegments();

    /**
     * Obtiene todos los segmentos por tipo
     */
    List<SegmentDto> getSegmentsByType(String segmentType);

    /**
     * Actualiza un segmento existente
     */
    SegmentDto updateSegment(Long id, SegmentDto segmentDto);

    /**
     * Elimina un segmento por su ID
     */
    void deleteSegment(Long id);

    /**
     * Activa o desactiva un segmento
     */
    SegmentDto setSegmentStatus(Long id, boolean active);

    /**
     * Obtiene los segmentos de un cliente
     */
    List<SegmentDto> getSegmentsByCustomerId(Long customerId);
}