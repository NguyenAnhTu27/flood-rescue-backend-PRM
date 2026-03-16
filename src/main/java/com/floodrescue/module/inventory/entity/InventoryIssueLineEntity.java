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
@Table(name = "inventory_issue_lines",
        indexes = {
                @Index(name = "idx_issue_line_issue", columnList = "issue_id"),
                @Index(name = "idx_issue_line_item", columnList = "item_category_id")
        }
)
public class InventoryIssueLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_issue_line_issue"))
    private InventoryIssueEntity issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_issue_line_item"))
    private ItemCategoryEntity itemCategory;

    @Column(name = "qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
}