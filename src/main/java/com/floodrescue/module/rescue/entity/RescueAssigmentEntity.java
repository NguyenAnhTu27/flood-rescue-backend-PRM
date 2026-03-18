package com.floodrescue.module.rescue.entity;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_assignments",
        indexes = {
                @Index(name = "idx_ra_group", columnList = "task_group_id"),
                @Index(name = "idx_ra_team", columnList = "team_id"),
                @Index(name = "idx_ra_asset", columnList = "asset_id"),
                @Index(name = "idx_ra_active", columnList = "is_active")
        }
)
public class RescueAssigmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ra_group"))
    private TaskGroupEntity taskGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ra_team"))
    private TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id",
            foreignKey = @ForeignKey(name = "fk_ra_asset"))
    private AssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by",
            foreignKey = @ForeignKey(name = "fk_ra_user"))
    private UserEntity assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
