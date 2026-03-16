package com.floodrescue.module.notification.entity;

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
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_read", columnList = "user_id, is_read, created_at"),
                @Index(name = "idx_notif_event_ref", columnList = "event_code, reference_type, reference_id")
        }
)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notif_user"))
    private UserEntity user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "event_code", length = 80)
    private String eventCode;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_urgent", nullable = false)
    @Builder.Default
    private Boolean isUrgent = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "action_status", length = 40)
    private String actionStatus;

    @Column(name = "action_note", columnDefinition = "TEXT")
    private String actionNote;

    @Column(name = "queue_request_id")
    private Long queueRequestId;

    @Column(name = "source_team_id")
    private Long sourceTeamId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (isUrgent == null) {
            isUrgent = false;
        }
    }
}
