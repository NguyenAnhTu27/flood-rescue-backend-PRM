package com.floodrescue.module.user.repository;

import com.floodrescue.module.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRole(@Param("id") Long id);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<UserEntity> findByTeamId(Long teamId);

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role r WHERE r.code = :roleCode ORDER BY u.id")
    List<UserEntity> findAllByRoleCode(@Param("roleCode") String roleCode);

    @Query("""
            SELECT u
            FROM UserEntity u
            JOIN FETCH u.role r
            WHERE u.teamId = :teamId
              AND r.code = :roleCode
            ORDER BY u.id
            """)
    List<UserEntity> findAllByTeamIdAndRoleCode(@Param("teamId") Long teamId, @Param("roleCode") String roleCode);

    @Query("""
            SELECT u
            FROM UserEntity u
            JOIN FETCH u.role r
            WHERE r.code = :roleCode
              AND u.rescueRequestBlocked = true
            ORDER BY u.id DESC
            """)
    List<UserEntity> findBlockedByRoleCode(@Param("roleCode") String roleCode);
}
