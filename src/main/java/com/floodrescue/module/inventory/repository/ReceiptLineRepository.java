package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.InventoryReceiptEntity;
import com.floodrescue.module.inventory.entity.InventoryReceiptLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptLineRepository extends JpaRepository<InventoryReceiptLineEntity, Long> {

    List<InventoryReceiptLineEntity> findByReceipt(InventoryReceiptEntity receipt);
}