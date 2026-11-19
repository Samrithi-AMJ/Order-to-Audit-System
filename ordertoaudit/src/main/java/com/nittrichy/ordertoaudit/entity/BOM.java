package com.nittrichy.ordertoaudit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BOM extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bom_code", unique = true, nullable = false, length = 30)
    private String bomCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_size", precision = 12, scale = 3, nullable = false)
    private BigDecimal batchSize; // e.g. 200 packets

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BOMItem> items = new ArrayList<>();

    public void addItem(BOMItem item) {
        items.add(item);
        item.setBom(this);
    }

    public void removeItem(BOMItem item) {
        items.remove(item);
        item.setBom(null);
    }
}
