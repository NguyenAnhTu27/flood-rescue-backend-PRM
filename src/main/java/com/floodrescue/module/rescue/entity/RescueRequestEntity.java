package com.floodrescue.module.rescue.entity;

import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_requests",
        indexes = {
                @Index(name = "idx_rr_citizen", columnList = "citizen_id"),
                @Index(name = "idx_rr_status_created", columnList = "status, created_at"),
                @Index(name = "idx_rr_priority_people_time", columnList = "priority, affected_people_count, created_at"),
                @Index(name = "idx_rr_master", columnList = "master_request_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rr_code", columnNames = "code")
        }
)
public class RescueRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rr_citizen"))
    private UserEntity citizen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RescueRequestStatus status = RescueRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RescuePriority priority = RescuePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_request_id",
            foreignKey = @ForeignKey(name = "fk_rr_master"))
    private RescueRequestEntity masterRequest;

    @Column(name = "affected_people_count", nullable = false)
    @Builder.Default
    private Integer affectedPeopleCount = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "address_text", length = 255)
    private String addressText;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_description", length = 500)
    private String locationDescription;

    @Column(name = "location_verified", nullable = false)
    @Builder.Default
    private Boolean locationVerified = false;

    @Column(name = "waiting_for_team", nullable = false)
    @Builder.Default
    private Boolean waitingForTeam = false;

    @Column(name = "coordinator_cancel_note", columnDefinition = "TEXT")
    private String coordinatorCancelNote;

    @Column(name = "is_emergency", nullable = false)
    @Builder.Default
    private Boolean isEmergency = false;

    @Column(name = "emergency_no")
    private Integer emergencyNo;

    @Column(name = "source_team_id")
    private Long sourceTeamId;

    @Column(name = "emergency_parent_request_id")
    private Long emergencyParentRequestId;

    @Column(name = "rescue_result_confirmation_status", length = 30)
    private String rescueResultConfirmationStatus;

    @Column(name = "rescue_result_confirmation_note", columnDefinition = "TEXT")
    private String rescueResultConfirmationNote;

    @Column(name = "rescue_result_confirmed_at")
    private LocalDateTime rescueResultConfirmedAt;

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
