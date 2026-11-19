package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.ProductionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_number", unique = true, nullable = false, length = 30)
    private String runNumber; // e.g. "PR-2026-0001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BOM bom;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "batch_multiplier", precision = 8, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal batchMultiplier = BigDecimal.ONE; // 1 batch = 200 packets

    @Column(name = "expected_output", precision = 12, scale = 3, nullable = false)
    private BigDecimal expectedOutput; // e.g. 200 packets

    @Column(name = "actual_good_output", precision = 12, scale = 3, nullable = false)
    private BigDecimal actualGoodOutput; // e.g. 188 packets

    @Column(name = "yield_variance_percentage", precision = 8, scale = 2)
    private BigDecimal yieldVariancePercentage; // e.g. -6.00%

    @Builder.Default
    @Column(name = "variance_flag", nullable = false)
    private boolean varianceFlag = false; // true if absolute variance > 3%

    @Builder.Default
    @Column(name = "labour_cost", precision = 14, scale = 2)
    private BigDecimal labourCost = BigDecimal.ZERO; // e.g. 1200.00

    @Builder.Default
    @Column(name = "overhead_cost", precision = 14, scale = 2)
    private BigDecimal overheadCost = BigDecimal.ZERO; // gas & power e.g. 600.00

    @Builder.Default
    @Column(name = "raw_material_cost", precision = 14, scale = 2)
    private BigDecimal rawMaterialCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "packaging_cost", precision = 14, scale = 2)
    private BigDecimal packagingCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_production_cost", precision = 14, scale = 2)
    private BigDecimal totalProductionCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cost_per_packet", precision = 14, scale = 4)
    private BigDecimal costPerPacket = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductionStatus status = ProductionStatus.COMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @Column(length = 255)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "productionRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialConsumption> consumptions = new ArrayList<>();

    public void addConsumption(MaterialConsumption consumption) {
        consumptions.add(consumption);
        consumption.setProductionRun(this);
    }

    public void removeConsumption(MaterialConsumption consumption) {
        consumptions.remove(consumption);
        consumption.setProductionRun(null);
    }
}
