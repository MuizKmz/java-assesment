package com.maybank.assessment.service;

import com.maybank.assessment.dto.request.CreateCustomerRequest;
import com.maybank.assessment.dto.request.UpdateCustomerRequest;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.dto.response.PageResponse;
import com.maybank.assessment.entity.CustomerStatus;
import org.springframework.data.domain.Sort;

public interface CustomerService {

    /** Fixed page size for the paginated listing API. */
    int PAGE_SIZE = 10;

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    CustomerResponse getCustomer(Long id);

    PageResponse<CustomerResponse> getCustomers(int page, CustomerStatus status, String sortBy, Sort.Direction direction);

    void deleteCustomer(Long id);
}
