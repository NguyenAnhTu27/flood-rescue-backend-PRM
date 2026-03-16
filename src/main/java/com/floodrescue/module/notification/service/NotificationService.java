package com.floodrescue.module.notification.service;

import com.floodrescue.module.notification.dto.EmergencyAckResponse;
import com.floodrescue.module.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    void notifyRole(
            String roleCode,
            String title,
            String content,
            String eventCode,
            String referenceType,
            Long referenceId,
            boolean urgent,
            Long sourceTeamId
    );

    void notifyUsers(
            List<Long> userIds,
            String title,
            String content,
            String eventCode,
            String referenceType,
            Long referenceId,
            boolean urgent,
            Long sourceTeamId
    );

    Page<NotificationResponse> getMyNotifications(Long userId, boolean unreadOnly, Pageable pageable);

    long countUnread(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    Long queueEmergencyRequest(Long userId, Long notificationId, boolean direct, String note);

    void markEmergencyConfirmed(Long coordinatorId, Long queueRequestId);

    void markEmergencyReassigned(Long coordinatorId, Long queueRequestId, String assignedTeamName);

    void markEmergencyOverloaded(Long coordinatorId, Long queueRequestId, String note);

    List<EmergencyAckResponse> getCoordinatorEmergencyAcks(Long taskGroupId);
}
