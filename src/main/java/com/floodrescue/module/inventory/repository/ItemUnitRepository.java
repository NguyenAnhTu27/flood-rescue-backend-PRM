package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.ItemUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemUnitRepository extends JpaRepository<ItemUnitEntity, Integer> {
    boolean existsByCode(String code);
    boolean existsByCodeOrName(String code, String name);
}
