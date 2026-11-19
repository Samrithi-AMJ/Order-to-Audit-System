package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Product;
import com.nittrichy.ordertoaudit.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(String productCode);
    Optional<Product> findByName(String name);
    List<Product> findByCategory(ProductCategory category);
    boolean existsByProductCode(String productCode);
}
