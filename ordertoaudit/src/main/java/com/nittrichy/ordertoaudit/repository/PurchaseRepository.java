package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Purchase;
import com.nittrichy.ordertoaudit.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);
    List<Purchase> findBySupplierId(Long supplierId);
    List<Purchase> findByStatus(PurchaseStatus status);
    List<Purchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);
}
