package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.response.RescuerDashboardResponse;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.rescue.dto.request.EscalateTaskGroupRequest;
import com.floodrescue.module.notification.dto.EmergencyAckResponse;
import com.floodrescue.shared.enums.TaskGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RescuerTaskService {
    RescuerDashboardResponse getDashboard(Long rescuerUserId);

    Page<TaskGroupResponse> getMyTaskGroups(Long rescuerUserId, TaskGroupStatus status, Pageable pageable);

    TaskGroupResponse getMyTaskGroup(Long rescuerUserId, Long taskGroupId);

    TaskGroupResponse updateMyTaskGroupStatus(Long rescuerUserId, Long taskGroupId, TaskGroupStatus status, String note);

    TaskGroupResponse escalateMyTaskGroup(Long rescuerUserId, Long taskGroupId, EscalateTaskGroupRequest request);

    java.util.List<EmergencyAckResponse> getEmergencyAcks(Long rescuerUserId, Long taskGroupId);

    void updateMyTeamLocation(Long rescuerUserId, Double latitude, Double longitude, String locationText);

    long returnMyTeamAssets(Long rescuerUserId);
}
