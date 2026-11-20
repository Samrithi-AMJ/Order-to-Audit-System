package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", unique = true, nullable = false, length = 30)
    private String productCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(nullable = false, length = 20)
    private String unit; // "PACKET", "BOX"

    @Column(name = "packet_weight_grams")
    private Integer packetWeightGrams; // e.g. 200

    @Column(name = "hsn_code", length = 20)
    private String hsnCode; // e.g. "1905"

    @Column(name = "selling_price", precision = 14, scale = 2, nullable = false)
    private BigDecimal sellingPrice; // e.g. 50.00

    @Builder.Default
    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal gstRate = new BigDecimal("5.00"); // 5%

    @Builder.Default
    @Column(name = "current_stock", precision = 12, scale = 3, nullable = false)
    private BigDecimal currentStock = BigDecimal.ZERO; // e.g. packets

    @Builder.Default
    @Column(name = "reorder_level", precision = 12, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

