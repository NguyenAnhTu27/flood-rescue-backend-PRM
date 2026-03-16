package com.floodrescue.module.rescue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_group_requests",
        indexes = {
                @Index(name = "idx_tgr_req", columnList = "rescue_request_id")
        }
)
public class TaskGroupRequestEntity {

    @EmbeddedId
    private TaskGroupRequestId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("taskGroupId")
    @JoinColumn(name = "task_group_id",
            foreignKey = @ForeignKey(name = "fk_tgr_group"))
    private TaskGroupEntity taskGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("rescueRequestId")
    @JoinColumn(name = "rescue_request_id",
            foreignKey = @ForeignKey(name = "fk_tgr_req"))
    private RescueRequestEntity rescueRequest;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class TaskGroupRequestId implements java.io.Serializable {
        @Column(name = "task_group_id")
        private Long taskGroupId;

        @Column(name = "rescue_request_id")
        private Long rescueRequestId;
    }
}
