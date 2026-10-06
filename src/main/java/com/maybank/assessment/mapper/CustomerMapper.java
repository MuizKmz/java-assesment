package com.maybank.assessment.mapper;

import com.maybank.assessment.dto.request.CreateCustomerRequest;
import com.maybank.assessment.dto.request.UpdateCustomerRequest;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.entity.Customer;
import com.maybank.assessment.entity.CustomerStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CustomerMapper {

    private static final String DEFAULT_CURRENCY = "MYR";

    public Customer toEntity(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.fullName().trim());
        customer.setEmail(normaliseEmail(request.email()));
        customer.setPhoneNo(request.phoneNo().trim());
        customer.setBalance(request.initialDeposit());
        customer.setCurrency(request.currency() == null ? DEFAULT_CURRENCY : request.currency());
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    public void updateEntity(Customer customer, UpdateCustomerRequest request) {
        customer.setFullName(request.fullName().trim());
        customer.setEmail(normaliseEmail(request.email()));
        customer.setPhoneNo(request.phoneNo().trim());
        customer.setStatus(request.status());
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhoneNo(),
                customer.getAccountNo(),
                customer.getBalance(),
                customer.getCurrency(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
