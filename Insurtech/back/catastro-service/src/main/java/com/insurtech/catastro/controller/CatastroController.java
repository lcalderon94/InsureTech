package com.insurtech.catastro.controller;

import com.insurtech.catastro.model.PropertyInfo;
import com.insurtech.catastro.service.CatastroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@Tag(name = "Catastro API")
public class CatastroController {

    private final CatastroService service;

    public CatastroController(CatastroService service) {
        this.service = service;
    }

    @Operation(summary = "Obtener información de un inmueble")
    @GetMapping("/{id}")
    public ResponseEntity<PropertyInfo> getById(@PathVariable String id) {
        PropertyInfo info = service.getProperty(id);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }
}
