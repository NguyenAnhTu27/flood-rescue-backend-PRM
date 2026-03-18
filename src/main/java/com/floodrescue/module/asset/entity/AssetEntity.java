package com.floodrescue.module.asset.entity;

import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.shared.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assets_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_assets_status", columnList = "status"),
                @Index(name = "idx_assets_team", columnList = "assigned_team_id")
        }
)
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id",
            foreignKey = @ForeignKey(name = "fk_assets_team"))
    private TeamEntity assignedTeam;

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
