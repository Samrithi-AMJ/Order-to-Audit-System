package com.nittrichy.ordertoaudit.entity;

import com.nittrichy.ordertoaudit.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bom_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BOMItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BOM bom;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType; // RAW_MATERIAL or PACKAGING_MATERIAL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_material_id")
    private PackagingMaterial packagingMaterial;

    @Column(name = "standard_quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal standardQuantity;

    @Column(nullable = false, length = 20)
    private String unit; // "KG", "LITRE", "PIECE", etc.

    @Column(length = 255)
    private String notes;
}
