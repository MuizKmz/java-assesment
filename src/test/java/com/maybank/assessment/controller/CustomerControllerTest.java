package com.maybank.assessment.controller;

import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.dto.response.PageResponse;
import com.maybank.assessment.entity.CustomerStatus;
import com.maybank.assessment.exception.ResourceNotFoundException;
import com.maybank.assessment.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void createCustomer_returns400WithFieldErrorsWhenInvalid() throws Exception {
        String body = """
                {"fullName": "", "email": "not-an-email", "phoneNo": "abc", "initialDeposit": -1}
                """;

        mockMvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNo").exists())
                .andExpect(jsonPath("$.fieldErrors.initialDeposit").exists());

        verifyNoInteractions(customerService);
    }

    @Test
    void createCustomer_returns201WithLocation() throws Exception {
        when(customerService.createCustomer(any())).thenReturn(sample(26L));
        String body = """
                {"fullName": "Ali bin Abu", "email": "ali@example.com", "phoneNo": "+60123456789", "initialDeposit": 100.50}
                """;

        mockMvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/customers/26"))
                .andExpect(jsonPath("$.data.id").value(26));
    }

    @Test
    void getCustomer_returns404WhenMissing() throws Exception {
        when(customerService.getCustomer(999L)).thenThrow(new ResourceNotFoundException("Customer", 999L));

        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer with id 999 not found"));
    }

    @Test
    void getCustomers_returnsPageEnvelope() throws Exception {
        PageResponse<CustomerResponse> page = new PageResponse<>(List.of(sample(11L)), 1, 10, 25, 3, false, false);
        when(customerService.getCustomers(eq(1), eq(null), eq("id"), eq(Sort.Direction.ASC))).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.content[0].id").value(11));
    }

    @Test
    void getCustomers_returns400ForNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/customers").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    private static CustomerResponse sample(Long id) {
        return new CustomerResponse(id, "Ali bin Abu", "ali@example.com", "+60123456789", "514012345678",
                new BigDecimal("100.50"), "MYR", CustomerStatus.ACTIVE, null, null);
    }
}
