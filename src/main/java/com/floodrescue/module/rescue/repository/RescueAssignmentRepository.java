package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.RescueAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueAssignmentRepository extends JpaRepository<RescueAssignmentEntity, Long> {

    List<RescueAssignmentEntity> findByTaskGroupIdAndIsActiveTrue(Long taskGroupId);

    List<RescueAssignmentEntity> findByTeamIdAndIsActiveTrue(Long teamId);
}
