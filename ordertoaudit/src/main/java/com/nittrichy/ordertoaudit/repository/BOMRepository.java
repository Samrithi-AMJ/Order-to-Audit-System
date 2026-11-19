package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.BOM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BOMRepository extends JpaRepository<BOM, Long> {
    Optional<BOM> findByBomCode(String bomCode);
    Optional<BOM> findByProductIdAndActiveTrue(Long productId);
    List<BOM> findByProductId(Long productId);
    boolean existsByBomCode(String bomCode);
}
