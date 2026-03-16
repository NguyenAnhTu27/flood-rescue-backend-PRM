package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.module.inventory.entity.InventoryIssueLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueLineRepository extends JpaRepository<InventoryIssueLineEntity, Long> {

    List<InventoryIssueLineEntity> findByIssue(InventoryIssueEntity issue);
}