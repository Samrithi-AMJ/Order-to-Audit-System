package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.MaterialConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialConsumptionRepository extends JpaRepository<MaterialConsumption, Long> {
    List<MaterialConsumption> findByProductionRunId(Long productionRunId);
    List<MaterialConsumption> findByVarianceFlagTrue();
}
