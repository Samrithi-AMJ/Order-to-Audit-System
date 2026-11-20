package com.nittrichy.ordertoaudit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", unique = true, nullable = false, length = 30)
    private String customerCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 50)
    private String state; // e.g., "Tamil Nadu", "Karnataka"

    @Column(name = "state_code", length = 5)
    private String stateCode; // e.g. "33" for TN, "29" for KA

    @Column(length = 20)
    private String gstin;

    @Builder.Default
    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "outstanding_balance", precision = 14, scale = 2, nullable = false)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_salesman_id")
    private User assignedSalesman;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

