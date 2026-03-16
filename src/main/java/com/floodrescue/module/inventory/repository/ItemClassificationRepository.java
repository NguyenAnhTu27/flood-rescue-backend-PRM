package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.ItemClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemClassificationRepository extends JpaRepository<ItemClassificationEntity, Integer> {
    boolean existsByCode(String code);
}
