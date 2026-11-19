package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "material_consumptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialConsumption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_run_id", nullable = false)
    private ProductionRun productionRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType; // RAW_MATERIAL or PACKAGING_MATERIAL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_material_id")
    private PackagingMaterial packagingMaterial;

    @Column(name = "planned_quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal plannedQuantity;

    @Column(name = "actual_quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal actualQuantity;

    @Builder.Default
    @Column(name = "damaged_quantity", precision = 12, scale = 3)
    private BigDecimal damagedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(name = "variance_percentage", precision = 8, scale = 2)
    private BigDecimal variancePercentage; // e.g. +5.00%

    @Builder.Default
    @Column(name = "variance_flag", nullable = false)
    private boolean varianceFlag = false; // true if absolute variance > 3%
}
