package com.floodrescue.module.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_code", columnNames = "code")
})
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String code; // CITIZEN/COORDINATOR/RESCUER/MANAGER/ADMIN

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}