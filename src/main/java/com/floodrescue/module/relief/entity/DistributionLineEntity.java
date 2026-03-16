package com.floodrescue.module.relief.entity;

import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "distribution_lines",
        indexes = {
                @Index(name = "idx_distribution_line_voucher", columnList = "distribution_id"),
                @Index(name = "idx_distribution_line_item", columnList = "item_category_id")
        })
public class DistributionLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_line_distribution"))
    private DistributionEntity distribution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_line_item"))
    private ItemCategoryEntity itemCategory;

    @Column(name = "qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
}
