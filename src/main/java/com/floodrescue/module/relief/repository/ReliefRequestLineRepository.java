package com.floodrescue.module.relief.repository;

import com.floodrescue.module.relief.entity.ReliefRequestEntity;
import com.floodrescue.module.relief.entity.ReliefRequestLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReliefRequestLineRepository extends JpaRepository<ReliefRequestLineEntity, Long> {

    List<ReliefRequestLineEntity> findByReliefRequest(ReliefRequestEntity reliefRequest);
}