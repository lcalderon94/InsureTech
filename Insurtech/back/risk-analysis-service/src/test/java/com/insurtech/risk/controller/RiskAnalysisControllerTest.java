package com.insurtech.risk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.risk.controller.RiskAnalysisController.RiskRequest;
import com.insurtech.risk.model.RiskEvaluation;
import com.insurtech.risk.service.RiskAnalysisService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RiskAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RiskAnalysisService service;

    @Test
    void analyzeEndpointWorks() throws Exception {
        when(service.analyze(anyString(), anyList())).thenReturn(new RiskEvaluation());
        RiskRequest request = new RiskRequest();
        request.setPolicyNumber("POL123");
        request.setRiskFactors(Arrays.asList(1.0, 2.0));
        mockMvc.perform(post("/risk/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
