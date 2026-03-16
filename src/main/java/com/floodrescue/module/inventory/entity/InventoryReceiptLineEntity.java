package com.floodrescue.module.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_receipt_lines",
        indexes = {
                @Index(name = "idx_receipt_line_receipt", columnList = "receipt_id"),
                @Index(name = "idx_receipt_line_item", columnList = "item_category_id")
        }
)
public class InventoryReceiptLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_line_receipt"))
    private InventoryReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_line_item"))
    private ItemCategoryEntity itemCategory;

    @Column(name = "qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
}