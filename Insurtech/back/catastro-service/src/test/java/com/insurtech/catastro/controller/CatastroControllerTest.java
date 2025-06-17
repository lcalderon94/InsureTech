package com.insurtech.catastro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.catastro.model.PropertyInfo;
import com.insurtech.catastro.service.CatastroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatastroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatastroService service;

    @Test
    void getByIdReturnsProperty() throws Exception {
        PropertyInfo property = new PropertyInfo("1", "Street 1", "Owner");
        when(service.getProperty(eq("1"))).thenReturn(property);
        mockMvc.perform(get("/properties/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(property)));
    }
}
