package com.insurtech.notification.util;

import com.insurtech.notification.exception.InvalidTemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para procesar plantillas y variables
 */
@Component
@Slf4j
public class TemplateParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Extrae las variables requeridas de una plantilla
     *
     * @param content contenido de la plantilla
     * @return lista de nombres de variables
     */
    public List<String> extractVariables(String content) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * Procesa una plantilla sustituyendo variables
     *
     * @param template contenido de la plantilla
     * @param variables mapa de variables y valores
     * @return contenido procesado
     */
    public String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            return "";
        }

        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.getOrDefault(variableName, "");

            // Convertir el valor a String, manejando nulos
            String replacement = (value != null) ? value.toString() : "";

            result = result.replace("${" + variableName + "}", replacement);
        }

        return result;
    }

    /**
     * Valida si todas las variables requeridas están presentes
     *
     * @param requiredVariables lista de variables requeridas
     * @param providedVariables mapa de variables proporcionadas
     * @return true si todas las variables requeridas están presentes
     */
    public boolean validateVariables(List<String> requiredVariables, Map<String, Object> providedVariables) {
        if (requiredVariables == null || requiredVariables.isEmpty()) {
            return true;
        }

        if (providedVariables == null || providedVariables.isEmpty()) {
            return false;
        }

        // Verificar cada variable requerida
        for (String var : requiredVariables) {
            if (!providedVariables.containsKey(var)) {
                log.warn("Variable requerida no proporcionada: {}", var);
                return false;
            }
        }

        return true;
    }

    /**
     * Valida si una plantilla tiene sintaxis correcta
     *
     * @param template contenido de la plantilla
     * @return true si la sintaxis es correcta
     */
    public boolean validateTemplateSyntax(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }

        try {
            // Verificar variables no cerradas
            int openCount = countOccurrences(template, "${");
            int closeCount = countOccurrences(template, "}");

            if (openCount != closeCount) {
                throw new InvalidTemplateException(
                        "Sintaxis de plantilla inválida: variables no balanceadas");
            }

            return true;
        } catch (Exception e) {
            log.error("Error validando sintaxis de plantilla: {}", e.getMessage());
            return false;
        }
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }
}