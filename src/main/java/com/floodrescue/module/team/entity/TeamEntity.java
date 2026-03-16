package com.floodrescue.module.team.entity;

import com.floodrescue.shared.enums.TeamType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "teams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teams_code", columnNames = "code")
        })
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;
    @Column(nullable = false, length = 120, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_type", nullable = false, length = 20)
    @Builder.Default
    private TeamType teamType = TeamType.RESCUE;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Byte status = (byte) 1;

    @Column(length = 255)
    private String description;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "current_location_text", length = 255)
    private String currentLocationText;

    @Column(name = "current_location_updated_at")
    private LocalDateTime currentLocationUpdatedAt;

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
