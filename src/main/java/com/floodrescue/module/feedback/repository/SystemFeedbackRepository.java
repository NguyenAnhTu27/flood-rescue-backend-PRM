package com.floodrescue.module.feedback.repository;

import com.floodrescue.module.feedback.entity.SystemFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SystemFeedbackRepository extends JpaRepository<SystemFeedbackEntity, Long> {

    @Query("SELECT AVG(sf.rating) FROM SystemFeedbackEntity sf")
    Double findAverageRating();

    long countByRating(Integer rating);
}
