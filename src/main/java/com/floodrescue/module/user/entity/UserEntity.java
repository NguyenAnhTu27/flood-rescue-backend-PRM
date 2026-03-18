package com.floodrescue.module.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_role", columnList = "role_id"),
                @Index(name = "idx_users_team", columnList = "team_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_phone", columnNames = "phone"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Role bắt buộc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role"))
    private RoleEntity role;

    // Citizen có thể không thuộc team => nullable
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private Byte status; // 1 active, 0 inactive

    @Column(name = "is_leader", nullable = false)
    @Builder.Default
    private Boolean isLeader = false; // true = leader của rescue team

    @Column(name = "rescue_request_blocked", nullable = false)
    @Builder.Default
    private Boolean rescueRequestBlocked = false;

    @Column(name = "rescue_request_blocked_reason", columnDefinition = "TEXT")
    private String rescueRequestBlockedReason;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
