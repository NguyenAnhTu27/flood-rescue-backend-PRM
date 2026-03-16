package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.request.CreateTaskGroupRequest;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.shared.enums.TaskGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskGroupService {

    TaskGroupResponse createTaskGroup(CreateTaskGroupRequest request, Long coordinatorId);

    TaskGroupResponse getTaskGroup(Long id);

    Page<TaskGroupResponse> getTaskGroups(TaskGroupStatus status, Pageable pageable);

    TaskGroupResponse changeStatus(Long id, TaskGroupStatus status, String note, Long coordinatorId);
}
