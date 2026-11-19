package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
}
