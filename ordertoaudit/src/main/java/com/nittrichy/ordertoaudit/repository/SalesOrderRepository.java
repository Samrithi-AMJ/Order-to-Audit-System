package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.SalesOrder;
import com.nittrichy.ordertoaudit.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    Optional<SalesOrder> findByOrderNumber(String orderNumber);
    List<SalesOrder> findByCustomerId(Long customerId);
    List<SalesOrder> findBySalesmanId(Long salesmanId);
    List<SalesOrder> findByStatus(OrderStatus status);
    List<SalesOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);
}
