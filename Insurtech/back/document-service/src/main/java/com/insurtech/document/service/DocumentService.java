package com.insurtech.document.service;

import com.insurtech.document.event.DocumentGeneratedEvent;
import com.insurtech.document.model.DocumentMetadata;
import com.insurtech.document.repository.DocumentMetadataRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final MinioClient minioClient;
    private final DocumentMetadataRepository repository;
    private final KafkaTemplate<String, DocumentGeneratedEvent> kafkaTemplate;

    private final String bucket = "documents";

    public DocumentMetadata upload(MultipartFile file) throws Exception {
        String objectName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        DocumentMetadata metadata = DocumentMetadata.builder()
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadDate(Instant.now())
                .bucket(bucket)
                .objectName(objectName)
                .build();
        metadata = repository.save(metadata);
        kafkaTemplate.send("document.generated", new DocumentGeneratedEvent(metadata));
        return metadata;
    }

    public Resource download(String id) throws Exception {
        DocumentMetadata metadata = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(metadata.getBucket())
                .object(metadata.getObjectName())
                .build());
        return new InputStreamResource(stream);
    }

    public DocumentMetadata generatePdf(String fileName, String content) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(content));
        document.close();
        return uploadInternal(fileName.endsWith(".pdf") ? fileName : fileName + ".pdf",
                "application/pdf",
                out.toByteArray());
    }

    public DocumentMetadata generateWord(String fileName, String content) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        doc.createParagraph().createRun().setText(content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return uploadInternal(fileName.endsWith(".docx") ? fileName : fileName + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                out.toByteArray());
    }

    private DocumentMetadata uploadInternal(String fileName, String contentType, byte[] data) throws Exception {
        String objectName = UUID.randomUUID() + "-" + fileName;
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, data.length, -1)
                    .contentType(contentType)
                    .build());
        }
        DocumentMetadata metadata = DocumentMetadata.builder()
                .fileName(fileName)
                .contentType(contentType)
                .size(data.length)
                .uploadDate(Instant.now())
                .bucket(bucket)
                .objectName(objectName)
                .build();
        metadata = repository.save(metadata);
        kafkaTemplate.send("document.generated", new DocumentGeneratedEvent(metadata));
        return metadata;
    }
}
