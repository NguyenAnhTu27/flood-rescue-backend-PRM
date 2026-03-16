package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.TaskGroupEntity;
import com.floodrescue.shared.enums.TaskGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TaskGroupRepository extends JpaRepository<TaskGroupEntity, Long> {

    Optional<TaskGroupEntity> findByCode(String code);

    Page<TaskGroupEntity> findByStatus(TaskGroupStatus status, Pageable pageable);

    Page<TaskGroupEntity> findByAssignedTeamId(Long teamId, Pageable pageable);

    Page<TaskGroupEntity> findByAssignedTeamIdAndStatus(Long teamId, TaskGroupStatus status, Pageable pageable);

    Page<TaskGroupEntity> findByAssignedTeamIdAndStatusIn(Long teamId, List<TaskGroupStatus> statuses, Pageable pageable);
}
