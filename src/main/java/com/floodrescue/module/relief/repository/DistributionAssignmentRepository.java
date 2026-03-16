package com.floodrescue.module.relief.repository;

import com.floodrescue.module.relief.entity.DistributionAssignmentEntity;
import com.floodrescue.module.relief.entity.DistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionAssignmentRepository extends JpaRepository<DistributionAssignmentEntity, Long> {

    List<DistributionAssignmentEntity> findByDistribution(DistributionEntity distribution);
}
