package com.maybank.assessment.service.impl;

import com.maybank.assessment.dto.request.CreateCustomerRequest;
import com.maybank.assessment.dto.request.UpdateCustomerRequest;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.dto.response.PageResponse;
import com.maybank.assessment.entity.Customer;
import com.maybank.assessment.entity.CustomerStatus;
import com.maybank.assessment.exception.DuplicateResourceException;
import com.maybank.assessment.exception.InvalidRequestException;
import com.maybank.assessment.exception.ResourceNotFoundException;
import com.maybank.assessment.mapper.CustomerMapper;
import com.maybank.assessment.repository.CustomerRepository;
import com.maybank.assessment.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, new CustomerMapper(), accountNumberGenerator);
    }

    @Test
    void createCustomer_savesNewActiveCustomerWithGeneratedAccountNo() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Ali bin Abu", " Ali@Example.com ", "+60123456789", new BigDecimal("1000.00"), null);
        when(customerRepository.existsByEmailIgnoreCase("ali@example.com")).thenReturn(false);
        when(accountNumberGenerator.generate()).thenReturn("514012345678");
        when(customerRepository.existsByAccountNo("514012345678")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ali@example.com");
        assertThat(response.accountNo()).isEqualTo("514012345678");
        assertThat(response.currency()).isEqualTo("MYR");
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void createCustomer_rejectsDuplicateEmail() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Ali bin Abu", "ali@example.com", "+60123456789", BigDecimal.TEN, "MYR");
        when(customerRepository.existsByEmailIgnoreCase("ali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_updatesMutableFields() {
        Customer existing = customer(5L);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 5L)).thenReturn(false);
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        CustomerResponse response = customerService.updateCustomer(5L, new UpdateCustomerRequest(
                "New Name", "NEW@example.com", "+60199999999", CustomerStatus.INACTIVE));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.status()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(response.accountNo()).isEqualTo(existing.getAccountNo());
    }

    @Test
    void getCustomer_throwsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getCustomers_alwaysUsesPageSizeOfTen() {
        Page<Customer> page = new PageImpl<>(List.of(customer(1L)), Pageable.ofSize(10), 25);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<CustomerResponse> result = customerService.getCustomers(1, null, "id", Sort.Direction.ASC);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void getCustomers_rejectsUnknownSortField() {
        assertThatThrownBy(() -> customerService.getCustomers(0, null, "password", Sort.Direction.ASC))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createCustomer_retriesWhenGeneratedAccountNoAlreadyExists() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Ali bin Abu", "ali@example.com", "+60123456789", BigDecimal.TEN, "MYR");
        when(customerRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(accountNumberGenerator.generate()).thenReturn("514000000001", "514000000002");
        when(customerRepository.existsByAccountNo("514000000001")).thenReturn(true);
        when(customerRepository.existsByAccountNo("514000000002")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(customerService.createCustomer(request).accountNo()).isEqualTo("514000000002");
    }

    private static Customer customer(Long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setFullName("Existing Customer");
        c.setEmail("existing@example.com");
        c.setPhoneNo("+60120000000");
        c.setAccountNo("514099999999");
        c.setBalance(new BigDecimal("500.00"));
        c.setCurrency("MYR");
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }
}
