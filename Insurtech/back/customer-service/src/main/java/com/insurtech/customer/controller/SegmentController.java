package com.insurtech.customer.controller;

import com.insurtech.customer.exception.ResourceNotFoundException;
import com.insurtech.customer.model.dto.SegmentDto;
import com.insurtech.customer.service.SegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segments")
@Tag(name = "Segment", description = "API para la gestión de segmentos de clientes")
@SecurityRequirement(name = "bearer-jwt")
public class SegmentController {

    private static final Logger log = LoggerFactory.getLogger(SegmentController.class);

    private final SegmentService segmentService;

    @Autowired
    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo segmento", description = "Crea un nuevo segmento de clientes")
    public ResponseEntity<SegmentDto> createSegment(@Valid @RequestBody SegmentDto segmentDto) {
        log.info("Creating segment: {}", segmentDto.getName());
        SegmentDto createdSegment = segmentService.createSegment(segmentDto);
        return new ResponseEntity<>(createdSegment, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener segmento por ID", description = "Obtiene un segmento por su ID")
    public ResponseEntity<SegmentDto> getSegmentById(@PathVariable Long id) {
        log.info("Getting segment by ID: {}", id);
        return segmentService.getSegmentById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con ID: " + id));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener segmento por nombre", description = "Obtiene un segmento por su nombre")
    public ResponseEntity<SegmentDto> getSegmentByName(@PathVariable String name) {
        log.info("Getting segment by name: {}", name);
        return segmentService.getSegmentByName(name)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Segmento no encontrado con nombre: " + name));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener todos los segmentos", description = "Obtiene todos los segmentos")
    public ResponseEntity<List<SegmentDto>> getAllSegments() {
        log.info("Getting all segments");
        List<SegmentDto> segments = segmentService.getAllSegments();
        return ResponseEntity.ok(segments);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener segmentos activos", description = "Obtiene todos los segmentos activos")
    public ResponseEntity<List<SegmentDto>> getAllActiveSegments() {
        log.info("Getting all active segments");
        List<SegmentDto> segments = segmentService.getAllActiveSegments();
        return ResponseEntity.ok(segments);
    }

    @GetMapping("/type/{segmentType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener segmentos por tipo", description = "Obtiene todos los segmentos de un tipo específico")
    public ResponseEntity<List<SegmentDto>> getSegmentsByType(@PathVariable String segmentType) {
        log.info("Getting segments by type: {}", segmentType);
        List<SegmentDto> segments = segmentService.getSegmentsByType(segmentType);
        return ResponseEntity.ok(segments);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar segmento", description = "Actualiza un segmento existente")
    public ResponseEntity<SegmentDto> updateSegment(
            @PathVariable Long id,
            @Valid @RequestBody SegmentDto segmentDto) {
        log.info("Updating segment with ID: {}", id);
        SegmentDto updatedSegment = segmentService.updateSegment(id, segmentDto);
        return ResponseEntity.ok(updatedSegment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar segmento", description = "Elimina un segmento por su ID")
    public ResponseEntity<Void> deleteSegment(@PathVariable Long id) {
        log.info("Deleting segment with ID: {}", id);
        segmentService.deleteSegment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar/desactivar segmento", description = "Activa o desactiva un segmento")
    public ResponseEntity<SegmentDto> setSegmentStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        log.info("Setting segment ID: {} active status to: {}", id, active);
        SegmentDto updatedSegment = segmentService.setSegmentStatus(id, active);
        return ResponseEntity.ok(updatedSegment);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener segmentos por cliente", description = "Obtiene todos los segmentos de un cliente")
    public ResponseEntity<List<SegmentDto>> getSegmentsByCustomerId(@PathVariable Long customerId) {
        log.info("Getting segments by customer ID: {}", customerId);
        List<SegmentDto> segments = segmentService.getSegmentsByCustomerId(customerId);
        return ResponseEntity.ok(segments);
    }
}