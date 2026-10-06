package com.maybank.assessment.controller;

import com.maybank.assessment.dto.request.CreateCustomerRequest;
import com.maybank.assessment.dto.request.UpdateCustomerRequest;
import com.maybank.assessment.dto.response.ApiResponse;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.dto.response.PageResponse;
import com.maybank.assessment.entity.CustomerStatus;
import com.maybank.assessment.exception.InvalidRequestException;
import com.maybank.assessment.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Customers", description = "Customer account CRUD and paginated listing")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create a customer (INSERT)")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse created = customerService.createCustomer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    @Operation(summary = "Update a customer (UPDATE)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Customer updated successfully", customerService.updateCustomer(id, request)));
    }

    @Operation(summary = "Get a customer by id (GET)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Customer retrieved successfully", customerService.getCustomer(id)));
    }

    @Operation(summary = "List customers with pagination (10 records per page)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getCustomers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        PageResponse<CustomerResponse> result =
                customerService.getCustomers(page, status, sortBy, parseDirection(direction));
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", result));
    }

    @Operation(summary = "Delete a customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable @Positive Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    private static Sort.Direction parseDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("direction must be 'asc' or 'desc'");
        }
    }
}
