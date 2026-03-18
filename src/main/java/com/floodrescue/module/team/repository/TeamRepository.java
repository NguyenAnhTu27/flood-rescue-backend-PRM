package com.floodrescue.module.team.repository;

import com.floodrescue.module.team.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    boolean existsByCode(String code);
    Optional<TeamEntity> findByCode(String code);

    boolean existsByName(String name);
    Optional<TeamEntity> findByName(String name);
}