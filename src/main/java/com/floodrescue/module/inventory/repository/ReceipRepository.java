package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.InventoryReceiptEntity;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceipRepository extends JpaRepository<InventoryReceiptEntity, Long> {

    Optional<InventoryReceiptEntity> findByCode(String code);

    Page<InventoryReceiptEntity> findByStatus(InventoryDocumentStatus status, Pageable pageable);
}
