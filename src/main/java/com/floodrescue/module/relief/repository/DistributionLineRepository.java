package com.floodrescue.module.relief.repository;

import com.floodrescue.module.relief.entity.DistributionEntity;
import com.floodrescue.module.relief.entity.DistributionLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionLineRepository extends JpaRepository<DistributionLineEntity, Long> {

    List<DistributionLineEntity> findByDistribution(DistributionEntity distribution);
}
