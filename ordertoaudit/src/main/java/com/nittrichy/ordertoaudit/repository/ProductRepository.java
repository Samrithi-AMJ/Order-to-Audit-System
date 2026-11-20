package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Product;
import com.nittrichy.ordertoaudit.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    Optional<Product> findByProductCode(String productCode);
    Optional<Product> findByName(String name);
    List<Product> findByCategory(ProductCategory category);
    boolean existsByProductCode(String productCode);
}

