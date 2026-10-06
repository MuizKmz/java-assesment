package com.maybank.assessment.repository;

import com.maybank.assessment.entity.Customer;
import com.maybank.assessment.entity.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByAccountNo(String accountNo);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
}
