package com.floodrescue.module.relief.reponsitory;

import com.floodrescue.module.relief.entity.ReliefRequestEntity;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReliefRequestRepository extends JpaRepository<ReliefRequestEntity, Long> {

    Page<ReliefRequestEntity> findByStatus(InventoryDocumentStatus status, Pageable pageable);

    Page<ReliefRequestEntity> findByCreatedByIdOrderByCreatedAtDesc(Long createdById, Pageable pageable);

    Page<ReliefRequestEntity> findByAssignedTeamIdOrderByUpdatedAtDesc(Long assignedTeamId, Pageable pageable);
}
