package com.floodrescue.module.asset.repository;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.shared.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetReponsitory extends JpaRepository<AssetEntity, Long> {

    List<AssetEntity> findByStatus(AssetStatus status);

    List<AssetEntity> findByAssignedTeamId(Long teamId);

    List<AssetEntity> findByAssignedTeamIdAndStatus(Long teamId, AssetStatus status);

    @Query("""
            select a from AssetEntity a
            left join fetch a.assignedTeam t
            where (:status is null or a.status = :status)
            """)
    List<AssetEntity> findAllWithTeam(@Param("status") AssetStatus status);
}
