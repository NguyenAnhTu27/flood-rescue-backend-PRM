package com.floodrescue.module.inventory.entity;

import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.StockSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_receipts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_receipt_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_receipt_status", columnList = "status"),
                @Index(name = "idx_receipt_created_by", columnList = "created_by")
        }
)
public class InventoryReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private StockSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryDocumentStatus status = InventoryDocumentStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private Long createdById;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}