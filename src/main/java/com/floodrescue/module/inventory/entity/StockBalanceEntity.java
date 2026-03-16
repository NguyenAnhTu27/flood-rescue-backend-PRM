package com.floodrescue.module.inventory.entity;

import com.floodrescue.shared.enums.StockSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_balances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_item_source", columnNames = {"item_category_id", "source_type"})
        },
        indexes = {
                @Index(name = "idx_stock_item", columnList = "item_category_id")
        }
)
public class StockBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_item"))
    private ItemCategoryEntity itemCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private StockSourceType sourceType;

    @Column(name = "qty", nullable = false, precision = 14, scale = 2)
    private BigDecimal qty;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}