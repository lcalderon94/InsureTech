package com.insurtech.document.event;

import com.insurtech.document.model.DocumentMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGeneratedEvent {
    private DocumentMetadata metadata;
}
