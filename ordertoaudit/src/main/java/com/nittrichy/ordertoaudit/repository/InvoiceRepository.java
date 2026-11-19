package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Invoice;
import com.nittrichy.ordertoaudit.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findBySalesOrderId(Long salesOrderId);
    List<Invoice> findByCustomerId(Long customerId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceNumber LIKE :prefix%")
    long countByInvoiceNumberPrefix(@Param("prefix") String prefix);
}
