package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.RescueRequestTimelineEntity;
import com.floodrescue.shared.enums.TimelineEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueTimelineRepository extends JpaRepository<RescueRequestTimelineEntity, Long> {

    List<RescueRequestTimelineEntity> findByRescueRequestIdOrderByCreatedAtDesc(Long rescueRequestId);

    Page<RescueRequestTimelineEntity> findByRescueRequestIdOrderByCreatedAtDesc(
            Long rescueRequestId,
            Pageable pageable
    );

    long countByRescueRequestIdAndEventType(Long rescueRequestId, TimelineEventType eventType);
}
