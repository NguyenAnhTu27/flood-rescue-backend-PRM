package com.floodrescue.module.rescue.entity;

import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.shared.enums.TaskGroupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_groups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tg_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_tg_status", columnList = "status"),
                @Index(name = "idx_tg_assigned_team", columnList = "assigned_team_id"),
                @Index(name = "idx_tg_created_by", columnList = "created_by")
        }
)
public class TaskGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskGroupStatus status = TaskGroupStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id",
            foreignKey = @ForeignKey(name = "fk_tg_team"))
    private TeamEntity assignedTeam;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @ForeignKey(name = "fk_tg_created_by"))
    private UserEntity createdBy;

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

