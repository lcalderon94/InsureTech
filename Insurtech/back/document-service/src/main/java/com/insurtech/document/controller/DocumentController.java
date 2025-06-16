package com.insurtech.document.controller;

import com.insurtech.document.model.DocumentMetadata;
import com.insurtech.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentMetadata upload(@RequestPart("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) throws Exception {
        Resource resource = service.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id)
                .body(resource);
    }

    @PostMapping("/generate/pdf")
    public DocumentMetadata generatePdf(@RequestParam String fileName, @RequestParam String content) throws Exception {
        return service.generatePdf(fileName, content);
    }

    @PostMapping("/generate/word")
    public DocumentMetadata generateWord(@RequestParam String fileName, @RequestParam String content) throws Exception {
        return service.generateWord(fileName, content);
    }
}
