package com.insurtech.customer.controller;

import com.insurtech.customer.model.dto.CustomerDto;
import com.insurtech.customer.service.CustomerService;
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

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCustomerByNumberReturnsOk() throws Exception {
        when(customerService.getCustomerByNumber("CUS123"))
                .thenReturn(Optional.of(new CustomerDto()));

        mockMvc.perform(get("/api/customers/number/CUS123"))
                .andExpect(status().isOk());
    }
}
