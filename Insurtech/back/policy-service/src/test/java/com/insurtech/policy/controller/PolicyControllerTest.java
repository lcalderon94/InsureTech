package com.insurtech.policy.controller;

import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PolicyService policyService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPolicyByNumberReturnsOk() throws Exception {
        when(policyService.getPolicyByNumber("POL123"))
                .thenReturn(Optional.of(new PolicyDto()));

        mockMvc.perform(get("/api/policies/number/POL123"))
                .andExpect(status().isOk());
    }
}
