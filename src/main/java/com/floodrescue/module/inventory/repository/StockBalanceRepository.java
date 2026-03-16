package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.entity.StockBalanceEntity;
import com.floodrescue.shared.enums.StockSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalanceEntity, Long> {

    Optional<StockBalanceEntity> findByItemCategoryAndSourceType(ItemCategoryEntity category, StockSourceType sourceType);

    List<StockBalanceEntity> findByItemCategory(ItemCategoryEntity category);
}