package com.insurtech.document.controller;

import com.insurtech.document.model.DocumentMetadata;
import com.insurtech.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService service;

    @Test
    void uploadReturnsOk() throws Exception {
        when(service.upload(any())).thenReturn(DocumentMetadata.builder().id("1").build());
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hi".getBytes());
        mockMvc.perform(multipart("/documents/upload").file(file))
                .andExpect(status().isOk());
    }
}
