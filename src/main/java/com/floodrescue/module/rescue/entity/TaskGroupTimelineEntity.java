package com.floodrescue.module.rescue.entity;

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
@Table(name = "task_group_timeline",
        indexes = {
                @Index(name = "idx_tg_tl_group_time", columnList = "task_group_id, created_at"),
                @Index(name = "idx_tg_tl_actor", columnList = "actor_id")
        }
)
public class TaskGroupTimelineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tg_tl_group"))
    private TaskGroupEntity taskGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tg_tl_actor"))
    private UserEntity actor;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

