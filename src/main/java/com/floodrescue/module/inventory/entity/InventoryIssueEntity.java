package com.floodrescue.module.inventory.entity;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.relief.entity.ReliefRequestEntity;
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
@Table(name = "inventory_issues",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_issue_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_issue_status", columnList = "status"),
                @Index(name = "idx_issue_created_by", columnList = "created_by"),
                @Index(name = "idx_issue_relief", columnList = "relief_request_id"),
                @Index(name = "idx_issue_team", columnList = "assigned_team_id"),
                @Index(name = "idx_issue_asset", columnList = "asset_id")
        }
)
public class InventoryIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryDocumentStatus status = InventoryDocumentStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private Long createdById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relief_request_id",
            foreignKey = @ForeignKey(name = "fk_issue_relief"))
    private ReliefRequestEntity reliefRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id",
            foreignKey = @ForeignKey(name = "fk_issue_team"))
    private TeamEntity assignedTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id",
            foreignKey = @ForeignKey(name = "fk_issue_asset"))
    private AssetEntity asset;

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