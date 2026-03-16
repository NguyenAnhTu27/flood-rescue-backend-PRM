package com.floodrescue.module.inventory.repository;

import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategoryEntity, Integer> {

    Optional<ItemCategoryEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            select ic from ItemCategoryEntity ic
            left join fetch ic.classification c
            where (:classificationId is null or c.id = :classificationId)
            order by ic.id desc
            """)
    List<ItemCategoryEntity> findAllWithClassification(@Param("classificationId") Integer classificationId);

    long countByClassification_Id(Integer classificationId);

    long countByUnitIgnoreCase(String unit);
}
