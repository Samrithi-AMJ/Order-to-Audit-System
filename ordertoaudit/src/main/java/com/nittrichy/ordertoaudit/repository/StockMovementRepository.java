package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.StockMovement;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByItemTypeOrderByMovementDateDesc(ItemType itemType);
    List<StockMovement> findByRawMaterialIdOrderByMovementDateDesc(Long rawMaterialId);
    List<StockMovement> findByPackagingMaterialIdOrderByMovementDateDesc(Long packagingMaterialId);
    List<StockMovement> findByProductIdOrderByMovementDateDesc(Long productId);
    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime start, LocalDateTime end);
    List<StockMovement> findByMovementType(MovementType movementType);

    Optional<StockMovement> findTopByRawMaterialIdOrderByMovementDateDescIdDesc(Long rawMaterialId);
    Optional<StockMovement> findTopByPackagingMaterialIdOrderByMovementDateDescIdDesc(Long packagingMaterialId);
    Optional<StockMovement> findTopByProductIdOrderByMovementDateDescIdDesc(Long productId);
}
