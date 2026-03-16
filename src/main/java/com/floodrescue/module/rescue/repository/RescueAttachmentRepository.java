package com.floodrescue.module.rescue.repository;

import com.floodrescue.module.rescue.entity.RescueRequestAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueAttachmentRepository extends JpaRepository<RescueRequestAttachmentEntity, Long> {

    List<RescueRequestAttachmentEntity> findByRescueRequestId(Long rescueRequestId);

    void deleteByRescueRequestId(Long rescueRequestId);
}
