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
import com.maybank.assessment.service.CustomerService;
import com.maybank.assessment.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final String RESOURCE = "Customer";
    private static final List<String> SORTABLE_FIELDS =
            List.of("id", "fullName", "email", "accountNo", "balance", "status", "createdAt", "updatedAt");
    private static final int MAX_ACCOUNT_NO_ATTEMPTS = 5;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AccountNumberGenerator accountNumberGenerator;

    /** INSERT - runs in a read/write transaction; any RuntimeException rolls the insert back. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String email = customerMapper.normaliseEmail(request.email());
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Customer with email " + email + " already exists");
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setAccountNo(generateUniqueAccountNo());

        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={} accountNo={}", saved.getId(), saved.getAccountNo());
        return customerMapper.toResponse(saved);
    }

    /** UPDATE - read/write transaction; @Version on the entity guards against lost updates. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomerOrThrow(id);

        String email = customerMapper.normaliseEmail(request.email());
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Customer with email " + email + " already exists");
        }

        customerMapper.updateEntity(customer, request);
        // Flush now so the response carries the new version / updatedAt values.
        Customer updated = customerRepository.saveAndFlush(customer);
        log.info("Updated customer id={}", id);
        return customerMapper.toResponse(updated);
    }

    /** GET - read-only transaction (no dirty checking / flush, can be routed to a replica). */
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return customerMapper.toResponse(findCustomerOrThrow(id));
    }

    /** GET with pagination - fixed page size of 10 records, read-only transaction. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(int page, CustomerStatus status, String sortBy,
                                                       Sort.Direction direction) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or greater");
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("sortBy must be one of " + SORTABLE_FIELDS);
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(direction, sortBy));
        Page<Customer> result = status == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findByStatus(status, pageable);

        return PageResponse.from(result.map(customerMapper::toResponse));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        Customer customer = findCustomerOrThrow(id);
        customerRepository.delete(customer);
        log.info("Deleted customer id={}", id);
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private String generateUniqueAccountNo() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NO_ATTEMPTS; attempt++) {
            String accountNo = accountNumberGenerator.generate();
            if (!customerRepository.existsByAccountNo(accountNo)) {
                return accountNo;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }
}
