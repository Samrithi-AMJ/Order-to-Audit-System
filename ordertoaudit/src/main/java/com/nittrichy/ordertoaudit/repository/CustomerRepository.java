package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByName(String name);
    List<Customer> findByAssignedSalesmanId(Long salesmanId);
    boolean existsByCustomerCode(String customerCode);
}

