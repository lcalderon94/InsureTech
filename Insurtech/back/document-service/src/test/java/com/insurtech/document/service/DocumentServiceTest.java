package com.insurtech.document.service;

import com.insurtech.document.event.DocumentGeneratedEvent;
import com.insurtech.document.model.DocumentMetadata;
import com.insurtech.document.repository.DocumentMetadataRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {

    @Test
    void uploadStoresFileAndMetadata() throws Exception {
        MinioClient minio = mock(MinioClient.class);
        DocumentMetadataRepository repo = mock(DocumentMetadataRepository.class);
        KafkaTemplate<String, DocumentGeneratedEvent> kafka = mock(KafkaTemplate.class);
        DocumentMetadata saved = DocumentMetadata.builder().id(UUID.randomUUID().toString()).build();
        when(repo.save(any())).thenReturn(saved);

        DocumentService service = new DocumentService(minio, repo, kafka);
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        DocumentMetadata metadata = service.upload(file);

        verify(minio).putObject(any(PutObjectArgs.class));
        verify(repo).save(any());
        verify(kafka).send(eq("document.generated"), any(DocumentGeneratedEvent.class));
        assertEquals(saved.getId(), metadata.getId());
    }
}
