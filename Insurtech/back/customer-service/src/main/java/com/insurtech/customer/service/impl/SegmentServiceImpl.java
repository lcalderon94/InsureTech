package com.insurtech.customer.service.impl;

import com.insurtech.customer.exception.ResourceNotFoundException;
import com.insurtech.customer.model.dto.SegmentDto;
import com.insurtech.customer.model.entity.Segment;
import com.insurtech.customer.repository.SegmentRepository;
import com.insurtech.customer.service.SegmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SegmentServiceImpl implements SegmentService {

    private static final Logger log = LoggerFactory.getLogger(SegmentServiceImpl.class);

    private final SegmentRepository segmentRepository;

    @Autowired
    public SegmentServiceImpl(SegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    @Override
    @Transactional
    public SegmentDto createSegment(SegmentDto segmentDto) {
        log.info("Creating segment: {}", segmentDto.getName());

        // Verificar que el nombre del segmento no existe ya
        if (segmentRepository.findByName(segmentDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un segmento con el nombre: " + segmentDto.getName());
        }

        Segment segment = mapToEntity(segmentDto);
        segment.setCreatedBy(getCurrentUsername());
        segment.setUpdatedBy(getCurrentUsername());

        segment = segmentRepository.save(segment);

        log.info("Segment created successfully with ID: {}", segment.getId());

        return mapToDto(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SegmentDto> getSegmentById(Long id) {
        log.debug("Getting segment by ID: {}", id);
        return segmentRepository.findById(id)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SegmentDto> getSegmentByName(String name) {
        log.debug("Getting segment by name: {}", name);
        return segmentRepository.findByName(name)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentDto> getAllSegments() {
        log.debug("Getting all segments");
        return segmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentDto> getAllActiveSegments() {
        log.debug("Getting all active segments");
        return segmentRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentDto> getSegmentsByType(String segmentType) {
        log.debug("Getting segments by type: {}", segmentType);

        try {
            Segment.SegmentType type = Segment.SegmentType.valueOf(segmentType);
            return segmentRepository.findBySegmentType(type).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de segmento inválido: " + segmentType);
        }
    }

    @Override
    @Transactional
    public SegmentDto updateSegment(Long id, SegmentDto segmentDto) {
        log.info("Updating segment with ID: {}", id);

        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + id));

        // Verificar que el nombre no colisione con otro segmento existente
        if (segmentDto.getName() != null &&
                !segment.getName().equals(segmentDto.getName()) &&
                segmentRepository.findByName(segmentDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un segmento con el nombre: " + segmentDto.getName());
        }

        // Actualizar campos
        if (segmentDto.getName() != null) segment.setName(segmentDto.getName());
        if (segmentDto.getDescription() != null) segment.setDescription(segmentDto.getDescription());
        if (segmentDto.getSegmentType() != null) segment.setSegmentType(segmentDto.getSegmentType());
        if (segmentDto.getSegmentCriteria() != null) segment.setSegmentCriteria(segmentDto.getSegmentCriteria());
        segment.setActive(segmentDto.isActive());

        segment.setUpdatedBy(getCurrentUsername());

        segment = segmentRepository.save(segment);

        log.info("Segment updated successfully with ID: {}", id);

        return mapToDto(segment);
    }

    @Override
    @Transactional
    public void deleteSegment(Long id) {
        log.info("Deleting segment with ID: {}", id);

        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + id));

        // Verificar si hay clientes en este segmento
        if (!segment.getCustomers().isEmpty()) {
            throw new IllegalStateException(
                    "No se puede eliminar el segmento porque tiene " +
                            segment.getCustomers().size() +
                            " clientes asociados");
        }

        segmentRepository.delete(segment);

        log.info("Segment deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public SegmentDto setSegmentStatus(Long id, boolean active) {
        log.info("Setting segment ID: {} active status to: {}", id, active);

        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + id));

        segment.setActive(active);
        segment.setUpdatedBy(getCurrentUsername());

        segment = segmentRepository.save(segment);

        log.info("Segment status updated successfully");

        return mapToDto(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentDto> getSegmentsByCustomerId(Long customerId) {
        log.debug("Getting segments by customer ID: {}", customerId);

        return segmentRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Métodos de mapeo

    private Segment mapToEntity(SegmentDto dto) {
        Segment segment = new Segment();
        segment.setId(dto.getId());
        segment.setName(dto.getName());
        segment.setDescription(dto.getDescription());
        segment.setSegmentType(dto.getSegmentType());
        segment.setSegmentCriteria(dto.getSegmentCriteria());
        segment.setActive(dto.isActive());
        return segment;
    }

    private SegmentDto mapToDto(Segment entity) {
        SegmentDto dto = new SegmentDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSegmentType(entity.getSegmentType());
        dto.setSegmentCriteria(entity.getSegmentCriteria());
        dto.setActive(entity.isActive());
        return dto;
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}