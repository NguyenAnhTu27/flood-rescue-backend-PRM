package com.floodrescue.module.relief.entity;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "distribution_assignments",
        indexes = {
                @Index(name = "idx_distribution_assignment_distribution", columnList = "distribution_id"),
                @Index(name = "idx_distribution_assignment_team", columnList = "team_id"),
                @Index(name = "idx_distribution_assignment_asset", columnList = "asset_id"),
                @Index(name = "idx_distribution_assignment_status", columnList = "status")
        })
public class DistributionAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_distribution_assignment_distribution"))
    private DistributionEntity distribution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id",
            foreignKey = @ForeignKey(name = "fk_distribution_assignment_team"))
    private TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id",
            foreignKey = @ForeignKey(name = "fk_distribution_assignment_asset"))
    private AssetEntity asset;

    @Column(name = "receiver_name", length = 120)
    private String receiverName;

    @Column(name = "receiver_phone", length = 30)
    private String receiverPhone;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "eta")
    private LocalDateTime eta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryDocumentStatus status = InventoryDocumentStatus.DRAFT;

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
