package com.insurtech.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class DocumentMetadata {
    @Id
    private String id;
    private String fileName;
    private String contentType;
    private long size;
    private Instant uploadDate;
    private String bucket;
    private String objectName;
}
