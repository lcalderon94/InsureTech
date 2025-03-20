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
            ClaimDocumentDto uploadedDocument = documentService.uploadDocumentByClaimNumber(
                    claimNumber, file, title, description, documentType);
            return new ResponseEntity<>(uploadedDocument, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error al subir documento", e);
            throw new RuntimeException("Error al subir documento: " + e.getMessage());
        }
    }

    @GetMapping("/claim/{claimNumber}/title/{title}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener documento por reclamación y título", description = "Obtiene un documento por número de reclamación y título")
    public ResponseEntity<ClaimDocumentDto> getDocumentByClaimNumberAndTitle(
            @PathVariable String claimNumber,
            @PathVariable String title) {
        log.info("Obteniendo documento con título: {} para reclamación número: {}", title, claimNumber);

        ClaimDocumentDto document = documentService.getDocumentByClaimNumberAndTitle(claimNumber, title)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado con título: " + title + " para reclamación: " + claimNumber));

        return ResponseEntity.ok(document);
    }

    @GetMapping("/claim/{claimNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener documentos por reclamación", description = "Obtiene todos los documentos de una reclamación")
    public ResponseEntity<List<ClaimDocumentDto>> getDocumentsByClaimNumber(@PathVariable String claimNumber) {
        log.info("Obteniendo documentos para reclamación número: {}", claimNumber);

        List<ClaimDocumentDto> documents = documentService.getDocumentsByClaimNumber(claimNumber);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/download/claim/{claimNumber}/title/{title}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Descargar documento", description = "Descarga un documento por número de reclamación y título")
    public ResponseEntity<byte[]> downloadDocumentByClaimNumberAndTitle(
            @PathVariable String claimNumber,
            @PathVariable String title) {
        log.info("Descargando documento con título: {} para reclamación número: {}", title, claimNumber);

        try {
            ClaimDocumentDto document = documentService.getDocumentByClaimNumberAndTitle(claimNumber, title)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Documento no encontrado con título: " + title + " para reclamación: " + claimNumber));

            byte[] fileContent = documentService.downloadDocumentByClaimNumberAndTitle(claimNumber, title);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getMimeType()))
                    .header("Content-Disposition", "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            log.error("Error al descargar documento", e);
            throw new RuntimeException("Error al descargar documento: " + e.getMessage());
        }
    }

    @PatchMapping("/claim/{claimNumber}/title/{title}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar documento", description = "Actualiza la información de un documento")
    public ResponseEntity<ClaimDocumentDto> updateDocumentByClaimNumberAndTitle(
            @PathVariable String claimNumber,
            @PathVariable String title,
            @Valid @RequestBody ClaimDocumentDto documentDto) {

        log.info("Actualizando documento con título: {} para reclamación número: {}", title, claimNumber);

        ClaimDocumentDto updatedDocument = documentService.updateDocumentByClaimNumberAndTitle(
                claimNumber, title, documentDto);

        return ResponseEntity.ok(updatedDocument);
    }

    @DeleteMapping("/claim/{claimNumber}/title/{title}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar documento", description = "Elimina un documento por número de reclamación y título")
    public ResponseEntity<Void> deleteDocumentByClaimNumberAndTitle(
            @PathVariable String claimNumber,
            @PathVariable String title) {
        log.info("Eliminando documento con título: {} para reclamación número: {}", title, claimNumber);

        documentService.deleteDocumentByClaimNumberAndTitle(claimNumber, title);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/claim/{claimNumber}/title/{title}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Verificar documento", description = "Marca un documento como verificado")
    public ResponseEntity<ClaimDocumentDto> verifyDocumentByClaimNumberAndTitle(
            @PathVariable String claimNumber,
            @PathVariable String title,
            @RequestParam boolean verified) {

        log.info("Marcando documento con título: {} para reclamación número: {} como verificado: {}",
                title, claimNumber, verified);

        ClaimDocumentDto updatedDocument = documentService.setDocumentVerificationStatusByClaimNumberAndTitle(
                claimNumber, title, verified);

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