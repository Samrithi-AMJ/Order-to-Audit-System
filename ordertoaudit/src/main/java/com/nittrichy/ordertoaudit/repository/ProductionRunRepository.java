package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.ProductionRun;
import com.nittrichy.ordertoaudit.enums.ProductionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRunRepository extends JpaRepository<ProductionRun, Long> {
    Optional<ProductionRun> findByRunNumber(String runNumber);
    List<ProductionRun> findByProductId(Long productId);
    List<ProductionRun> findByStatus(ProductionStatus status);
    List<ProductionRun> findByRunDateBetween(LocalDate startDate, LocalDate endDate);
}
