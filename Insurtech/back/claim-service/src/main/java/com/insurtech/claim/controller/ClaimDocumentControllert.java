package com.insurtech.claim.controller;

import com.insurtech.claim.exception.ClaimNotFoundException;
import com.insurtech.claim.exception.ResourceNotFoundException;
import com.insurtech.claim.model.dto.ClaimDocumentDto;
import com.insurtech.claim.model.entity.ClaimDocument;
import com.insurtech.claim.service.ClaimDocumentService;
import com.insurtech.claim.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/claims/documents")
@Tag(name = "Documentos de Reclamaciones", description = "API para la gestión de documentos de reclamaciones")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ClaimDocumentController {

    private static final Logger log = LoggerFactory.getLogger(ClaimDocumentController.class);

    private final ClaimService claimService;
    private final ClaimDocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Subir documento", description = "Carga un documento para una reclamación")
    public ResponseEntity<ClaimDocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("claimNumber") String claimNumber,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("documentType") ClaimDocument.DocumentType documentType) {

        log.info("Cargando documento {} para reclamación número: {}", title, claimNumber);

        try {
            return claimService.getClaimByNumber(claimNumber)
                    .map(claim -> {
                        try {
                            ClaimDocumentDto uploadedDocument = documentService.uploadDocument(
                                    claim.getId(), file, title, description, documentType);
                            return new ResponseEntity<>(uploadedDocument, HttpStatus.CREATED);
                        } catch (IOException e) {
                            log.error("Error al procesar el archivo", e);
                            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage());
                        }
                    })
                    .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
        } catch (Exception e) {
            log.error("Error al subir documento", e);
            throw new RuntimeException("Error al subir documento: " + e.getMessage());
        }
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener documento", description = "Obtiene un documento por su ID")
    public ResponseEntity<ClaimDocumentDto> getDocument(@PathVariable Long documentId) {
        log.info("Obteniendo documento con ID: {}", documentId);

        ClaimDocumentDto document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        return ResponseEntity.ok(document);
    }

    @GetMapping("/claim/{claimNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener documentos por reclamación", description = "Obtiene todos los documentos de una reclamación")
    public ResponseEntity<List<ClaimDocumentDto>> getDocumentsByClaim(@PathVariable String claimNumber) {
        log.info("Obteniendo documentos para reclamación número: {}", claimNumber);

        return claimService.getClaimByNumber(claimNumber)
                .map(claim -> {
                    List<ClaimDocumentDto> documents = documentService.getClaimDocuments(claim.getId());
                    return ResponseEntity.ok(documents);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @GetMapping("/download/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Descargar documento", description = "Descarga un documento por su ID")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        log.info("Descargando documento con ID: {}", documentId);

        return documentService.getDocumentById(documentId)
                .map(document -> {
                    try {
                        byte[] fileContent = documentService.downloadDocument(documentId);

                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(document.getMimeType()))
                                .header("Content-Disposition", "attachment; filename=\"" + document.getFileName() + "\"")
                                .body(fileContent);
                    } catch (Exception e) {
                        log.error("Error al descargar documento", e);
                        throw new RuntimeException("Error al descargar documento: " + e.getMessage());
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));
    }

    @PatchMapping("/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar documento", description = "Actualiza la información de un documento")
    public ResponseEntity<ClaimDocumentDto> updateDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody ClaimDocumentDto documentDto) {

        log.info("Actualizando documento con ID: {}", documentId);

        ClaimDocumentDto updatedDocument = documentService.updateDocument(documentId, documentDto);

        return ResponseEntity.ok(updatedDocument);
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar documento", description = "Elimina un documento por su ID")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        log.info("Eliminando documento con ID: {}", documentId);

        documentService.deleteDocument(documentId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{documentId}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Verificar documento", description = "Marca un documento como verificado")
    public ResponseEntity<ClaimDocumentDto> verifyDocument(
            @PathVariable Long documentId,
            @RequestParam boolean verified) {

        log.info("Marcando documento ID: {} como verificado: {}", documentId, verified);

        ClaimDocumentDto updatedDocument = documentService.setDocumentVerificationStatus(documentId, verified);

        return ResponseEntity.ok(updatedDocument);
    }

    @GetMapping("/types/{documentType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener documentos por tipo", description = "Obtiene documentos de un tipo específico")
    public ResponseEntity<List<ClaimDocumentDto>> getDocumentsByType(
            @PathVariable ClaimDocument.DocumentType documentType) {

        log.info("Obteniendo documentos de tipo: {}", documentType);

        List<ClaimDocumentDto> documents = documentService.getDocumentsByType(documentType);

        return ResponseEntity.ok(documents);
    }
}