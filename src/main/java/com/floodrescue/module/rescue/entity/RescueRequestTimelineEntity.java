package com.floodrescue.module.rescue.entity;

import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TimelineEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_request_timeline",
        indexes = {
                @Index(name = "idx_rr_tl_req_time", columnList = "rescue_request_id, created_at"),
                @Index(name = "idx_rr_tl_actor", columnList = "actor_id")
        }
)
public class RescueRequestTimelineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rr_tl_req"))
    private RescueRequestEntity rescueRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rr_tl_actor"))
    private UserEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TimelineEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RescueRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private RescueRequestStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
