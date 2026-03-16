package com.floodrescue.module.asset.repository;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    boolean existsByCode(String code);

    List<AssetEntity> findByStatus(AssetStatus status);

    List<AssetEntity> findByAssignedTeamIdAndStatus(Long teamId, AssetStatus status);

    List<AssetEntity> findByAssignedTeamId(Long teamId);

    @Query("""
            select a from AssetEntity a
            left join fetch a.assignedTeam t
            where (:status is null or a.status = :status)
            """)
    List<AssetEntity> findAllWithTeam(@Param("status") AssetStatus status);

    @Query("""
            select a from AssetEntity a
            left join fetch a.assignedTeam t
            where a.id = :id
            """)
    Optional<AssetEntity> findByIdWithTeam(@Param("id") Long id);

    @Query("""
            select a from AssetEntity a
            left join fetch a.assignedTeam t
            where (:status is null or a.status = :status)
            and (:assetType is null or a.assetType = :assetType)
            and (:teamId is null or a.assignedTeam.id = :teamId)
            """)
    List<AssetEntity> findAllWithFilters(
            @Param("status") AssetStatus status,
            @Param("assetType") AssetType assetType,
            @Param("teamId") Long teamId);
}
