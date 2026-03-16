package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.TaskGroupTimelineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskGroupTimelineRepository extends JpaRepository<TaskGroupTimelineEntity, Long> {

    List<TaskGroupTimelineEntity> findByTaskGroupIdOrderByCreatedAtDesc(Long taskGroupId);
}

