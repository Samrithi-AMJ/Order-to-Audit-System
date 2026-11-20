package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.MaterialCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "raw_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", unique = true, nullable = false, length = 30)
    private String materialCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaterialCategory category;

    @Column(nullable = false, length = 20)
    private String unit; // "KG", "LITRE", "GRAM"

    @Builder.Default
    @Column(name = "current_stock", precision = 12, scale = 3, nullable = false)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "weighted_avg_cost", precision = 14, scale = 4, nullable = false)
    private BigDecimal weightedAvgCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "reorder_level", precision = 12, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

