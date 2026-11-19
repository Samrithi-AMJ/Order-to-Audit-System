package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
    List<SalesOrderItem> findBySalesOrderId(Long salesOrderId);
    List<SalesOrderItem> findByProductId(Long productId);
}
