package com.floodrescue.module.relief.entity;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.shared.enums.DistributionPriority;
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
@Table(name = "distributions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_distribution_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_distribution_status", columnList = "status"),
                @Index(name = "idx_distribution_relief_request", columnList = "relief_request_id"),
                @Index(name = "idx_distribution_issue", columnList = "issue_id"),
                @Index(name = "idx_distribution_team", columnList = "team_id"),
                @Index(name = "idx_distribution_asset", columnList = "asset_id")
        })
public class DistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InventoryDocumentStatus status = InventoryDocumentStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private Long createdById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relief_request_id",
            foreignKey = @ForeignKey(name = "fk_distribution_relief_request"))
    private ReliefRequestEntity reliefRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id",
            foreignKey = @ForeignKey(name = "fk_distribution_issue"))
    private InventoryIssueEntity issue;

    @Column(name = "issue_ref_code", length = 40)
    private String issueRefCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id",
            foreignKey = @ForeignKey(name = "fk_distribution_team"))
    private TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id",
            foreignKey = @ForeignKey(name = "fk_distribution_asset"))
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
    @Column(name = "priority", length = 20)
    @Builder.Default
    private DistributionPriority priority = DistributionPriority.TRUNG_BINH;

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
