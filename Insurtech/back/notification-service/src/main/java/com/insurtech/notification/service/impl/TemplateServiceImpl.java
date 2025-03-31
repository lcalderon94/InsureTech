package com.insurtech.notification.service.impl;

import com.insurtech.notification.exception.InvalidTemplateException;
import com.insurtech.notification.exception.NotificationException;
import com.insurtech.notification.exception.TemplateNotFoundException;
import com.insurtech.notification.model.dto.TemplateDto;
import com.insurtech.notification.model.entity.NotificationTemplate;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.repository.NotificationTemplateRepository;
import com.insurtech.notification.service.interfaces.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final NotificationTemplateRepository templateRepository;

    // Patrón para variables ${variable} en plantillas
    private final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public TemplateDto createTemplate(TemplateDto templateDto) {
        // Verificar si ya existe código
        if (templateRepository.existsByCode(templateDto.getCode())) {
            throw new InvalidTemplateException("Ya existe una plantilla con el código: " + templateDto.getCode());
        }

        // Extraer variables requeridas
        List<String> requiredVariables = extractRequiredVariables(templateDto.getContent());

        NotificationTemplate template = NotificationTemplate.builder()
                .code(templateDto.getCode())
                .name(templateDto.getName())
                .type(templateDto.getType())
                .subject(templateDto.getSubject())
                .content(templateDto.getContent())
                .isActive(templateDto.getIsActive() == null ? true : templateDto.getIsActive())
                .requiredVariables(String.join(",", requiredVariables))
                .description(templateDto.getDescription())
                .eventType(templateDto.getEventType())
                .createdAt(LocalDateTime.now())
                .build();

        template = templateRepository.save(template);
        return mapToDto(template);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public TemplateDto updateTemplate(UUID id, TemplateDto templateDto) {
        NotificationTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + id));

        // Verificar si el nuevo código ya existe (si se cambió)
        if (!existingTemplate.getCode().equals(templateDto.getCode()) &&
                templateRepository.existsByCode(templateDto.getCode())) {
            throw new InvalidTemplateException("Ya existe una plantilla con el código: " + templateDto.getCode());
        }

        // Extraer variables requeridas
        List<String> requiredVariables = extractRequiredVariables(templateDto.getContent());

        existingTemplate.setCode(templateDto.getCode());
        existingTemplate.setName(templateDto.getName());
        existingTemplate.setType(templateDto.getType());
        existingTemplate.setSubject(templateDto.getSubject());
        existingTemplate.setContent(templateDto.getContent());
        existingTemplate.setIsActive(templateDto.getIsActive());
        existingTemplate.setRequiredVariables(String.join(",", requiredVariables));
        existingTemplate.setDescription(templateDto.getDescription());
        existingTemplate.setEventType(templateDto.getEventType());
        existingTemplate.setUpdatedAt(LocalDateTime.now());

        existingTemplate = templateRepository.save(existingTemplate);
        return mapToDto(existingTemplate);
    }

    @Override
    @Cacheable(value = "templates", key = "#id", unless = "#result == null")
    public Optional<TemplateDto> findTemplateById(UUID id) {
        return templateRepository.findById(id)
                .map(this::mapToDto);
    }

    @Override
    @Cacheable(value = "templates", key = "#code", unless = "#result == null")
    public Optional<NotificationTemplate> findTemplateByCode(String code) {
        return templateRepository.findByCode(code);
    }

    @Override
    public Page<TemplateDto> findAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public Page<TemplateDto> searchTemplates(String name, String code,
                                             NotificationType type, Boolean active,
                                             String eventType, Pageable pageable) {
        return templateRepository.searchTemplates(name, code, type, active, eventType, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Cacheable(value = "templates", key = "'type:' + #type", unless = "#result.isEmpty()")
    public List<TemplateDto> findActiveTemplatesByType(NotificationType type) {
        return templateRepository.findActiveTemplatesByType(type)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "templates", key = "'eventType:' + #eventType", unless = "#result.isEmpty()")
    public List<TemplateDto> findActiveTemplatesByEventType(String eventType) {
        return templateRepository.findActiveTemplatesByEventType(eventType)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public String processTemplate(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || templateContent.isBlank()) {
            return "";
        }

        if (variables == null) {
            variables = new HashMap<>();
        }

        String result = templateContent;
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = (value != null) ? value.toString() : "";

            result = result.replace("${" + variableName + "}", replacement);
        }

        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public boolean deleteTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            return false;
        }

        templateRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public TemplateDto toggleTemplateStatus(UUID id, boolean active) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + id));

        template.setIsActive(active);
        template = templateRepository.save(template);

        return mapToDto(template);
    }

    @Override
    public boolean validateTemplateVariables(UUID templateId, Map<String, Object> variables) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Plantilla no encontrada: " + templateId));

        if (template.getRequiredVariables() == null || template.getRequiredVariables().isBlank()) {
            return true;
        }

        List<String> requiredVars = Arrays.asList(template.getRequiredVariables().split(","));

        if (variables == null) {
            return requiredVars.isEmpty();
        }

        // Verificar que todas las variables requeridas estén presentes
        return requiredVars.stream().allMatch(variables::containsKey);
    }

    private List<String> extractRequiredVariables(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    private TemplateDto mapToDto(NotificationTemplate template) {
        List<String> requiredVars = Collections.emptyList();
        if (template.getRequiredVariables() != null && !template.getRequiredVariables().isBlank()) {
            requiredVars = Arrays.asList(template.getRequiredVariables().split(","));
        }

        return TemplateDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .type(template.getType())
                .subject(template.getSubject())
                .content(template.getContent())
                .isActive(template.getIsActive())
                .requiredVariables(requiredVars)
                .description(template.getDescription())
                .eventType(template.getEventType())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}