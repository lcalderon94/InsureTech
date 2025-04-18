package com.insurtech.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.notification.exception.NotificationException;
import com.insurtech.notification.exception.TemplateNotFoundException;
import com.insurtech.notification.model.dto.BatchRequestDto;
import com.insurtech.notification.model.dto.DeliveryAttemptDto;
import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.dto.NotificationResponseDto;
import com.insurtech.notification.model.entity.DeliveryAttempt;
import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.entity.NotificationBatch;
import com.insurtech.notification.model.entity.NotificationTemplate;
import com.insurtech.notification.model.enums.DeliveryStatus;
import com.insurtech.notification.model.enums.NotificationStatus;
import com.insurtech.notification.repository.DeliveryAttemptRepository;
import com.insurtech.notification.repository.NotificationBatchRepository;
import com.insurtech.notification.repository.NotificationRepository;
import com.insurtech.notification.repository.NotificationTemplateRepository;
import com.insurtech.notification.service.interfaces.EmailService;
import com.insurtech.notification.service.interfaces.NotificationService;
import com.insurtech.notification.service.interfaces.SmsService;
import com.insurtech.notification.service.interfaces.TemplateService;
import com.insurtech.notification.util.NotificationNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationBatchRepository batchRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateService templateService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;
    private final NotificationNumberGenerator numberGenerator;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Override
    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto requestDto) {
        log.info("Creando notificación para: {}, tipo: {}", requestDto.getRecipient(), requestDto.getType());

        Notification notification = buildNotificationFromRequest(requestDto);
        notification = notificationRepository.save(notification);

        // Procesamos inmediatamente si no está programada
        if (notification.getScheduledTime() == null ||
                notification.getScheduledTime().isBefore(LocalDateTime.now())) {
            processNotificationAsync(notification);
        }

        return mapToResponseDto(notification);
    }

    @Override
    @Transactional
    public UUID createBatch(BatchRequestDto batchRequestDto) {
        log.info("Creando lote de notificaciones con {} elementos",
                batchRequestDto.getNotifications().size());

        NotificationBatch batch = NotificationBatch.builder()
                .batchNumber(numberGenerator.generateBatchNumber())
                .status(NotificationStatus.PENDING)
                .totalNotifications(batchRequestDto.getNotifications().size())
                .processedCount(0)
                .successCount(0)
                .failedCount(0)
                .sourceReference(batchRequestDto.getSourceReference())
                .sourceType(batchRequestDto.getSourceType())
                .description(batchRequestDto.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        batch = batchRepository.save(batch);

        final UUID batchId = batch.getId();

        // Procesar notificaciones en segundo plano
        processBatchAsync(batchRequestDto.getNotifications(), batchId);

        return batchId;
    }

    @Override
    @Cacheable(value = "notifications", key = "#id", unless = "#result == null")
    public Optional<NotificationResponseDto> findNotificationById(UUID id) {
        return notificationRepository.findById(id)
                .map(this::mapToResponseDto);
    }

    @Override
    @Cacheable(value = "notifications", key = "#notificationNumber", unless = "#result == null")
    public Optional<NotificationResponseDto> findNotificationByNumber(String notificationNumber) {
        return notificationRepository.findByNotificationNumber(notificationNumber)
                .map(this::mapToResponseDto);
    }

    @Override
    public Page<NotificationResponseDto> findAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public Page<NotificationResponseDto> searchNotifications(
            String recipient, String subject, NotificationStatus status,
            String type, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {

        return notificationRepository.searchNotifications(
                        recipient, subject, status, type, fromDate, toDate, pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public boolean cancelNotification(UUID id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);

        if (notificationOpt.isEmpty()) {
            return false;
        }

        Notification notification = notificationOpt.get();

        if (notification.getStatus() != NotificationStatus.PENDING &&
                notification.getStatus() != NotificationStatus.SCHEDULED) {
            return false;
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        notificationRepository.save(notification);

        return true;
    }

    @Override
    @Transactional
    public NotificationResponseDto retryNotification(UUID id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);

        if (notificationOpt.isEmpty()) {
            throw new NotificationException("Notificación no encontrada: " + id);
        }

        Notification notification = notificationOpt.get();

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new NotificationException("Solo se pueden reintentar notificaciones fallidas");
        }

        // Resetear estado para procesar nuevamente
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        // Procesamos en segundo plano
        processNotificationAsync(notification);

        return mapToResponseDto(notification);
    }

    @Override
    @Transactional
    public boolean processNotification(Notification notification) {
        log.info("Procesando notificación: {}", notification.getNotificationNumber());

        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notification = notificationRepository.save(notification);

            DeliveryStatus deliveryStatus;
            String statusMessage = null;
            String providerMessageId = null;

            // Obtener el número de intento actual
            Integer attemptNumber = deliveryAttemptRepository.findMaxAttemptNumberByNotificationId(notification.getId());
            attemptNumber = (attemptNumber == null) ? 1 : attemptNumber + 1;

            try {
                // Enviar según tipo de notificación
                switch (notification.getType()) {
                    case EMAIL:
                        deliveryStatus = emailService.sendEmail(notification);
                        break;
                    case SMS:
                        deliveryStatus = smsService.sendSms(notification);
                        break;
                    default:
                        throw new NotificationException("Tipo de notificación no soportado: " + notification.getType());
                }
            } catch (Exception e) {
                deliveryStatus = DeliveryStatus.FAILED;
                statusMessage = e.getMessage();
                log.error("Error al enviar notificación {}: {}",
                        notification.getNotificationNumber(), e.getMessage());
            }

            // Registrar intento de entrega
            DeliveryAttempt attempt = DeliveryAttempt.builder()
                    .notification(notification)
                    .attemptNumber(attemptNumber)
                    .status(deliveryStatus)
                    .statusMessage(statusMessage)
                    .providerMessageId(providerMessageId)
                    .attemptTime(LocalDateTime.now())
                    .build();

            deliveryAttemptRepository.save(attempt);

            // Actualizar estado de la notificación
            boolean success = deliveryStatus == DeliveryStatus.SENT ||
                    deliveryStatus == DeliveryStatus.DELIVERED;

            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentTime(LocalDateTime.now());
            } else {
                // Determinar si se puede reintentar
                if (attemptNumber < maxRetryAttempts) {
                    notification.setStatus(NotificationStatus.PENDING);
                    // Programar siguiente intento con backoff exponencial
                    LocalDateTime nextRetry = calculateNextRetryTime(attemptNumber);
                    attempt.setNextRetryTime(nextRetry);
                    deliveryAttemptRepository.save(attempt);
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                }
            }

            notification = notificationRepository.save(notification);

            // Actualizar estado del lote si pertenece a uno
            if (notification.getBatch() != null) {
                updateBatchStatus(notification.getBatch().getId());
            }

            return success;
        } catch (Exception e) {
            log.error("Error procesando notificación {}: {}",
                    notification.getNotificationNumber(), e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            return false;
        }
    }

    @Override
    @Transactional
    public int processScheduledNotifications() {
        List<Notification> scheduledNotifications = notificationRepository.findScheduledNotificationsToProcess(
                NotificationStatus.SCHEDULED, LocalDateTime.now());

        int processed = 0;

        for (Notification notification : scheduledNotifications) {
            try {
                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);
                processNotificationAsync(notification);
                processed++;
            } catch (Exception e) {
                log.error("Error procesando notificación programada {}: {}",
                        notification.getNotificationNumber(), e.getMessage());
            }
        }

        return processed;
    }

    @Override
    @Transactional
    public int processRetryNotifications() {
        List<DeliveryAttempt> attemptsToRetry = deliveryAttemptRepository.findAttemptsToRetry(
                DeliveryStatus.FAILED, LocalDateTime.now());

        int processed = 0;

        for (DeliveryAttempt attempt : attemptsToRetry) {
            try {
                Notification notification = attempt.getNotification();
                if (notification.getStatus() == NotificationStatus.PENDING) {
                    processNotificationAsync(notification);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Error procesando reintento para la notificación {}: {}",
                        attempt.getNotification().getNotificationNumber(), e.getMessage());
            }
        }

        return processed;
    }

    @Override
    public List<NotificationResponseDto> findBySourceEventId(String sourceEventId) {
        return notificationRepository.findBySourceEventId(sourceEventId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationBatch> findBatchById(UUID batchId) {
        return batchRepository.findById(batchId);
    }

    @Override
    @Transactional
    public NotificationBatch updateBatchStatus(UUID batchId) {
        Optional<NotificationBatch> batchOpt = batchRepository.findById(batchId);

        if (batchOpt.isEmpty()) {
            throw new NotificationException("Lote no encontrado: " + batchId);
        }

        NotificationBatch batch = batchOpt.get();
        batch.updateCounts();

        return batchRepository.save(batch);
    }

    private Notification buildNotificationFromRequest(NotificationRequestDto requestDto) {
        Notification notification = Notification.builder()
                .notificationNumber(numberGenerator.generateNotificationNumber())
                .type(requestDto.getType())
                .priority(requestDto.getPriority())
                .status(requestDto.getScheduledTime() != null &&
                        requestDto.getScheduledTime().isAfter(LocalDateTime.now())
                        ? NotificationStatus.SCHEDULED : NotificationStatus.PENDING)
                .recipient(requestDto.getRecipient())
                .scheduledTime(requestDto.getScheduledTime())
                .sourceEventId(requestDto.getSourceEventId())
                .sourceEventType(requestDto.getSourceEventType())
                .build();

        // Manejar CC recipients
        if (requestDto.getCcRecipients() != null && !requestDto.getCcRecipients().isEmpty()) {
            notification.setCcRecipients(String.join(",", requestDto.getCcRecipients()));
        }

        // Si tiene código de plantilla, cargar plantilla
        if (requestDto.getTemplateCode() != null && !requestDto.getTemplateCode().isBlank()) {
            try {
                Optional<NotificationTemplate> templateOpt =
                        templateService.findTemplateByCode(requestDto.getTemplateCode());

                if (templateOpt.isEmpty()) {
                    throw new TemplateNotFoundException("Plantilla no encontrada: " + requestDto.getTemplateCode());
                }

                // Extraer los datos que necesitamos sin mantener referencia al objeto template
                String subject = templateOpt.get().getSubject();
                String templateContent = templateOpt.get().getContent();
                UUID templateId = templateOpt.get().getId();

                // Configurar la notificación con estos datos
                notification.setSubject(subject);

                // Procesar contenido con variables
                String processedContent = templateService.processTemplate(
                        templateContent, requestDto.getTemplateVariables());
                notification.setContent(processedContent);

                // Obtener una instancia "fresca" del template usando el ID
                // Esto evita el problema de classloading
                try {
                    // Si tienes acceso al repositorio
                    NotificationTemplate freshTemplate = templateRepository.getReferenceById(templateId);
                    notification.setTemplate(freshTemplate);
                } catch (Exception e) {
                    log.warn("No se pudo obtener referencia fresca del template: {}", e.getMessage());
                    // La aplicación puede continuar sin la referencia al template
                    // ya que ya tenemos el subject y content procesados
                }

                // Guardar variables como JSON para referencia
                try {
                    if (requestDto.getTemplateVariables() != null) {
                        notification.setDataContext(
                                objectMapper.writeValueAsString(requestDto.getTemplateVariables()));
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Error serializando variables de plantilla: {}", e.getMessage());
                }
            } catch (ClassCastException e) {
                log.warn("Error de classloader detectado: {}", e.getMessage());

                // Usar content y subject directos como fallback
                notification.setSubject(requestDto.getSubject() != null ?
                        requestDto.getSubject() : "Notificación");
                notification.setContent(requestDto.getContent() != null ?
                        requestDto.getContent() : "Contenido de notificación");
            }
        } else {
            // Sin plantilla, usar contenido directo
            notification.setSubject(requestDto.getSubject());
            notification.setContent(requestDto.getContent());
        }

        return notification;
    }

    private NotificationResponseDto mapToResponseDto(Notification notification) {
        List<DeliveryAttemptDto> attempts = new ArrayList<>();

        // Verificar que deliveryAttempts no sea null antes de hacer stream
        if (notification.getDeliveryAttempts() != null) {
            attempts = notification.getDeliveryAttempts().stream()
                    .map(this::mapDeliveryAttemptToDto)
                    .collect(Collectors.toList());
        }

        List<String> ccList = null;
        if (notification.getCcRecipients() != null && !notification.getCcRecipients().isBlank()) {
            ccList = Arrays.asList(notification.getCcRecipients().split(","));
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .notificationNumber(notification.getNotificationNumber())
                .type(notification.getType())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .recipient(notification.getRecipient())
                .ccRecipients(ccList)
                .templateCode(notification.getTemplate() != null ? notification.getTemplate().getCode() : null)
                .sourceEventId(notification.getSourceEventId())
                .sourceEventType(notification.getSourceEventType())
                .scheduledTime(notification.getScheduledTime())
                .sentTime(notification.getSentTime())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .deliveryAttempts(attempts)
                .build();
    }

    private DeliveryAttemptDto mapDeliveryAttemptToDto(DeliveryAttempt attempt) {
        return DeliveryAttemptDto.builder()
                .id(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus().name())
                .statusMessage(attempt.getStatusMessage())
                .attemptTime(attempt.getAttemptTime())
                .build();
    }

    private LocalDateTime calculateNextRetryTime(int attemptNumber) {
        // Implementación simple de backoff exponencial
        int delayMinutes = (int) Math.pow(2, attemptNumber - 1);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    @Async("notificationTaskExecutor")
    protected CompletableFuture<Boolean> processNotificationAsync(Notification notification) {
        boolean result = processNotification(notification);
        return CompletableFuture.completedFuture(result);
    }

    @Async("batchTaskExecutor")
    protected void processBatchAsync(List<NotificationRequestDto> notifications, UUID batchId) {
        log.info("Procesando lote {} con {} notificaciones", batchId, notifications.size());

        Optional<NotificationBatch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isEmpty()) {
            log.error("Lote no encontrado: {}", batchId);
            return;
        }

        NotificationBatch batch = batchOpt.get();

        for (NotificationRequestDto requestDto : notifications) {
            try {
                Notification notification = buildNotificationFromRequest(requestDto);
                notification.setBatch(batch);
                notification = notificationRepository.save(notification);

                // Procesamos inmediatamente si no está programada
                if (notification.getScheduledTime() == null ||
                        notification.getScheduledTime().isBefore(LocalDateTime.now())) {
                    processNotification(notification);
                }
            } catch (Exception e) {
                log.error("Error procesando notificación en lote {}: {}", batchId, e.getMessage());
            }
        }

        // Actualizar estado del lote
        updateBatchStatus(batchId);
    }
}