package com.insurtech.document.repository;

import com.insurtech.document.model.DocumentMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentMetadataRepository extends MongoRepository<DocumentMetadata, String> {
}
