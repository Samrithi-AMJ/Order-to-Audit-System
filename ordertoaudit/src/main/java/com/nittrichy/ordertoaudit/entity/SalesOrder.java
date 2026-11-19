package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesman_id")
    private User salesman;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_APPROVAL;

    @Builder.Default
    @Column(name = "total_taxable_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalTaxableAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_gst_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalGstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "grand_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(length = 255)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderItem> items = new ArrayList<>();

    public void addItem(SalesOrderItem item) {
        items.add(item);
        item.setSalesOrder(this);
    }

    public void removeItem(SalesOrderItem item) {
        items.remove(item);
        item.setSalesOrder(null);
    }
}
