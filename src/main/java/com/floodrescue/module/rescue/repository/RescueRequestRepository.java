package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RescueRequestRepository extends JpaRepository<RescueRequestEntity, Long>, JpaSpecificationExecutor<RescueRequestEntity> {

    Optional<RescueRequestEntity> findByCode(String code);

    boolean existsByCode(String code);

    Page<RescueRequestEntity> findByStatus(RescueRequestStatus status, Pageable pageable);

    Page<RescueRequestEntity> findByCitizenId(Long citizenId, Pageable pageable);

    Page<RescueRequestEntity> findByStatusAndPriorityOrderByAffectedPeopleCountDescCreatedAtAsc(
            RescueRequestStatus status,
            RescuePriority priority,
            Pageable pageable
    );

    List<RescueRequestEntity> findByMasterRequestId(Long masterRequestId);

    long countByEmergencyParentRequestId(Long emergencyParentRequestId);

    @Query("""
            SELECT r
            FROM RescueRequestEntity r
            WHERE r.status = :status
            ORDER BY 
              CASE r.priority
                WHEN com.floodrescue.shared.enums.RescuePriority.HIGH THEN 3
                WHEN com.floodrescue.shared.enums.RescuePriority.MEDIUM THEN 2
                WHEN com.floodrescue.shared.enums.RescuePriority.LOW THEN 1
                ELSE 0
              END DESC,
              r.affectedPeopleCount DESC,
              r.createdAt ASC
            """)
    Page<RescueRequestEntity> findPendingRequestsOrderedByPriority(
            @Param("status") RescueRequestStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT rq
            FROM RescueRequestEntity rq
            JOIN TaskGroupRequestEntity tgr ON rq.id = tgr.rescueRequest.id
            JOIN RescueAssignmentEntity ra ON tgr.taskGroup.id = ra.taskGroup.id
            WHERE ra.team.id = :teamId AND ra.isActive = true
            """)
    Page<RescueRequestEntity> findActiveRequestsByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    List<RescueRequestEntity> findByStatusAndLatitudeIsNotNull(RescueRequestStatus status);
}
