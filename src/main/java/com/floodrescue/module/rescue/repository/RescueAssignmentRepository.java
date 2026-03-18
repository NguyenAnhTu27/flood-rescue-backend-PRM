package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.RescueAssigmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueAssignmentRepository extends JpaRepository<RescueAssigmentEntity, Long> {

    List<RescueAssigmentEntity> findByTaskGroupIdAndIsActiveTrue(Long taskGroupId);

    List<RescueAssigmentEntity> findByTeamIdAndIsActiveTrue(Long teamId);
}
