package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<InventoryIssueEntity, Long> {

    Optional<InventoryIssueEntity> findByCode(String code);

    Page<InventoryIssueEntity> findByStatus(InventoryDocumentStatus status, Pageable pageable);

    Optional<InventoryIssueEntity> findFirstByReliefRequestIdOrderByIdDesc(Long reliefRequestId);

    List<InventoryIssueEntity> findByStatusOrderByUpdatedAtDesc(InventoryDocumentStatus status);
}
