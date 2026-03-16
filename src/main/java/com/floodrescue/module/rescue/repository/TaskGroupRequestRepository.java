package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskGroupRequestRepository extends JpaRepository<TaskGroupRequestEntity, TaskGroupRequestEntity.TaskGroupRequestId> {

    List<TaskGroupRequestEntity> findByTaskGroupId(Long taskGroupId);

    List<TaskGroupRequestEntity> findByRescueRequestId(Long rescueRequestId);

    void deleteByRescueRequestId(Long rescueRequestId);
}
