package com.insurtech.payment.controller;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPaymentReturnsCreated() throws Exception {
        PaymentDto dto = new PaymentDto();
        dto.setCustomerNumber("C1");
        dto.setPaymentType(Payment.PaymentType.ONLINE);
        dto.setConcept("Test");
        dto.setAmount(java.math.BigDecimal.ONE);
        dto.setCurrency("USD");

        when(paymentService.createPayment(any(PaymentDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}
