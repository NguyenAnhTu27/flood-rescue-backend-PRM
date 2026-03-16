package com.floodrescue.module.notification.repository;

import com.floodrescue.module.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    long countByUserIdAndIsReadFalse(Long userId);

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<NotificationEntity> findFirstByUserIdAndEventCodeAndReferenceTypeAndReferenceIdOrderByIdDesc(
            Long userId,
            String eventCode,
            String referenceType,
            Long referenceId
    );

    @Query("""
        SELECT n
        FROM NotificationEntity n
        JOIN FETCH n.user u
        JOIN FETCH u.role r
        WHERE n.eventCode = :eventCode
          AND n.referenceType = :referenceType
          AND n.referenceId = :referenceId
          AND r.code = 'COORDINATOR'
        ORDER BY u.id ASC
    """)
    List<NotificationEntity> findCoordinatorNotificationsByRef(
            @Param("eventCode") String eventCode,
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId
    );

    List<NotificationEntity> findByEventCodeAndQueueRequestId(String eventCode, Long queueRequestId);

    List<NotificationEntity> findByEventCodeAndQueueRequestIdIn(String eventCode, List<Long> queueRequestIds);
}
