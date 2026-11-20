package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    Optional<Supplier> findByName(String name);
    boolean existsBySupplierCode(String supplierCode);
}

