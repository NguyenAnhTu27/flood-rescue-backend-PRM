package com.floodrescue.module.relief.repository;

import com.floodrescue.module.relief.entity.DistributionEntity;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributionRepository extends JpaRepository<DistributionEntity, Long> {

    Optional<DistributionEntity> findByCode(String code);

    Page<DistributionEntity> findByStatus(InventoryDocumentStatus status, Pageable pageable);
}
