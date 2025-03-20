package com.insurtech.claim.service.impl;

import com.insurtech.claim.exception.ClaimNotFoundException;
import com.insurtech.claim.exception.ResourceNotFoundException;
import com.insurtech.claim.model.dto.ClaimDocumentDto;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimDocument;
import com.insurtech.claim.repository.ClaimDocumentRepository;
import com.insurtech.claim.repository.ClaimRepository;
import com.insurtech.claim.service.ClaimDocumentService;
import com.insurtech.claim.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimDocumentServiceImpl implements ClaimDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ClaimDocumentServiceImpl.class);

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final EntityDtoMapper mapper;

    // Configuración para almacenamiento de archivos
    private final String uploadDir = "uploads/claims";

    @Override
    @Transactional
    public ClaimDocumentDto uploadDocument(
            Long claimId,
            MultipartFile file,
            String title,
            String description,
            ClaimDocument.DocumentType documentType) throws IOException {

        log.info("Cargando documento para reclamación ID: {}", claimId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con ID: " + claimId));

        // Validar archivo
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No se puede cargar un archivo vacío");
        }

        // Obtener nombre del archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        // Crear carpeta si no existe
        String claimFolder = uploadDir + "/" + claim.getClaimNumber();
        Path uploadPath = Paths.get(claimFolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Guardar archivo en el sistema de archivos
        Path targetLocation = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Crear entidad ClaimDocument
        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setTitle(title);
        document.setDescription(description);
        document.setDocumentType(documentType);
        document.setFileName(newFilename);
        document.setFilePath(claimFolder + "/" + newFilename);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setVerified(false);
        document.setUploadDate(LocalDateTime.now());
        document.setUploadedBy(getCurrentUsername());
        document.setCreatedBy(getCurrentUsername());

        document = documentRepository.save(document);

        log.info("Documento cargado exitosamente. ID: {}", document.getId());

        return mapper.toDto(document);
    }

    @Override
    @Transactional
    public ClaimDocumentDto uploadDocumentByClaimNumber(
            String claimNumber,
            MultipartFile file,
            String title,
            String description,
            ClaimDocument.DocumentType documentType) throws IOException {

        log.info("Cargando documento para reclamación número: {}", claimNumber);

        // Verificar si ya existe un documento con el mismo título
        Optional<ClaimDocument> existingDocument = documentRepository.findByClaimNumberAndTitle(claimNumber, title);
        if (existingDocument.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un documento con el título: " + title + " para la reclamación: " + claimNumber);
        }

        Claim claim = claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));

        return uploadDocument(claim.getId(), file, title, description, documentType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClaimDocumentDto> getDocumentById(Long documentId) {
        log.debug("Obteniendo documento con ID: {}", documentId);
        return documentRepository.findById(documentId)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClaimDocumentDto> getDocumentByClaimNumberAndTitle(String claimNumber, String title) {
        log.debug("Obteniendo documento con título: {} para reclamación número: {}", title, claimNumber);

        return documentRepository.findByClaimNumberAndTitle(claimNumber, title)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDocumentDto> getClaimDocuments(Long claimId) {
        log.debug("Obteniendo documentos para reclamación ID: {}", claimId);
        return documentRepository.findByClaimId(claimId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDocumentDto> getDocumentsByClaimNumber(String claimNumber) {
        log.debug("Obteniendo documentos para reclamación número: {}", claimNumber);

        return documentRepository.findByClaimNumber(claimNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(Long documentId) throws IOException {
        log.info("Descargando documento con ID: {}", documentId);

        ClaimDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("El archivo no existe en el sistema de archivos: " + document.getFilePath());
        }

        return Files.readAllBytes(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocumentByClaimNumberAndTitle(String claimNumber, String title) throws IOException {
        log.info("Descargando documento con título: {} para reclamación número: {}", title, claimNumber);

        // Obtener el documento
        ClaimDocument document = findDocumentEntityByClaimNumberAndTitle(claimNumber, title);

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("El archivo no existe en el sistema de archivos: " + document.getFilePath());
        }

        return Files.readAllBytes(filePath);
    }

    @Override
    @Transactional
    public ClaimDocumentDto updateDocument(Long documentId, ClaimDocumentDto documentDto) {
        log.info("Actualizando documento con ID: {}", documentId);

        ClaimDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        // Actualizar campos editables
        if (documentDto.getTitle() != null) document.setTitle(documentDto.getTitle());
        if (documentDto.getDescription() != null) document.setDescription(documentDto.getDescription());
        if (documentDto.getDocumentType() != null) document.setDocumentType(documentDto.getDocumentType());
        if (documentDto.getExternalUrl() != null) document.setExternalUrl(documentDto.getExternalUrl());
        document.setVerified(documentDto.isVerified());

        document = documentRepository.save(document);

        log.info("Documento actualizado con éxito. ID: {}", documentId);

        return mapper.toDto(document);
    }

    @Override
    @Transactional
    public ClaimDocumentDto updateDocumentByClaimNumberAndTitle(String claimNumber, String title, ClaimDocumentDto documentDto) {
        log.info("Actualizando documento con título: {} para reclamación número: {}", title, claimNumber);

        // Obtener el documento
        ClaimDocument document = findDocumentEntityByClaimNumberAndTitle(claimNumber, title);

        // Validar si se está cambiando el título y ya existe otro documento con ese título
        if (documentDto.getTitle() != null && !documentDto.getTitle().equals(document.getTitle())) {
            Optional<ClaimDocument> existingDocument =
                    documentRepository.findByClaimNumberAndTitle(claimNumber, documentDto.getTitle());
            if (existingDocument.isPresent()) {
                throw new IllegalArgumentException(
                        "Ya existe un documento con el título: " + documentDto.getTitle() +
                                " para la reclamación: " + claimNumber);
            }
        }

        // Actualizar campos editables
        if (documentDto.getTitle() != null) document.setTitle(documentDto.getTitle());
        if (documentDto.getDescription() != null) document.setDescription(documentDto.getDescription());
        if (documentDto.getDocumentType() != null) document.setDocumentType(documentDto.getDocumentType());
        if (documentDto.getExternalUrl() != null) document.setExternalUrl(documentDto.getExternalUrl());
        document.setVerified(documentDto.isVerified());

        document = documentRepository.save(document);

        log.info("Documento actualizado con éxito. ID: {}", document.getId());

        return mapper.toDto(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("Eliminando documento con ID: {}", documentId);

        ClaimDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        // Eliminar archivo físico si existe
        try {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico: {}", e.getMessage());
            // Continuar con la eliminación de la entidad
        }

        // Eliminar entidad
        documentRepository.delete(document);

        log.info("Documento eliminado con éxito. ID: {}", documentId);
    }

    @Override
    @Transactional
    public void deleteDocumentByClaimNumberAndTitle(String claimNumber, String title) {
        log.info("Eliminando documento con título: {} para reclamación número: {}", title, claimNumber);

        // Obtener el documento
        ClaimDocument document = findDocumentEntityByClaimNumberAndTitle(claimNumber, title);

        // Eliminar archivo físico si existe
        try {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico: {}", e.getMessage());
            // Continuar con la eliminación de la entidad
        }

        // Eliminar entidad
        documentRepository.delete(document);

        log.info("Documento eliminado con éxito. ID: {}", document.getId());
    }

    @Override
    @Transactional
    public ClaimDocumentDto setDocumentVerificationStatus(Long documentId, boolean verified) {
        log.info("Estableciendo estado de verificación para documento ID: {} a: {}", documentId, verified);

        ClaimDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        document.setVerified(verified);
        document = documentRepository.save(document);

        log.info("Estado de verificación actualizado. ID: {}", documentId);

        return mapper.toDto(document);
    }

    @Override
    @Transactional
    public ClaimDocumentDto setDocumentVerificationStatusByClaimNumberAndTitle(String claimNumber, String title, boolean verified) {
        log.info("Estableciendo estado de verificación para documento con título: {} para reclamación número: {} a: {}",
                title, claimNumber, verified);

        // Obtener el documento
        ClaimDocument document = findDocumentEntityByClaimNumberAndTitle(claimNumber, title);

        document.setVerified(verified);
        document = documentRepository.save(document);

        log.info("Estado de verificación actualizado. ID: {}", document.getId());

        return mapper.toDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDocumentDto> getDocumentsByType(ClaimDocument.DocumentType documentType) {
        log.debug("Obteniendo documentos de tipo: {}", documentType);

        List<ClaimDocument> documents = documentRepository.findByDocumentType(documentType);

        return documents.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene documentos por número de reclamación y tipo de documento
     */
    @Transactional(readOnly = true)
    public List<ClaimDocumentDto> getDocumentsByClaimNumberAndType(String claimNumber, ClaimDocument.DocumentType documentType) {
        log.debug("Obteniendo documentos de tipo: {} para reclamación número: {}", documentType, claimNumber);

        List<ClaimDocument> documents = documentRepository.findByClaimNumberAndDocumentType(claimNumber, documentType);

        return documents.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene documentos verificados por número de reclamación
     */
    @Transactional(readOnly = true)
    public List<ClaimDocumentDto> getVerifiedDocumentsByClaimNumber(String claimNumber) {
        log.debug("Obteniendo documentos verificados para reclamación número: {}", claimNumber);

        List<ClaimDocument> documents = documentRepository.findByClaimNumberAndVerifiedTrue(claimNumber);

        return documents.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // Métodos auxiliares privados

    /**
     * Obtiene la extensión de un archivo a partir de su nombre
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return ""; // Sin extensión
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * Obtiene el nombre de usuario actual
     */
    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * Encuentra un documento por número de reclamación y título
     */
    private ClaimDocument findDocumentEntityByClaimNumberAndTitle(String claimNumber, String title) {
        return documentRepository.findByClaimNumberAndTitle(claimNumber, title)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento no encontrado con título: " + title + " para reclamación: " + claimNumber));
    }
}