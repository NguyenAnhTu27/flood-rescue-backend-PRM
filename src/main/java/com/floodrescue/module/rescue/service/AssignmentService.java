package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.request.AssignTaskGroupRequest;
import com.floodrescue.module.rescue.dto.response.AssignmentResponse;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;

public interface AssignmentService {

    TaskGroupResponse assignTaskGroup(AssignTaskGroupRequest request, Long coordinatorId);

    AssignmentResponse getLatestActiveAssignment(Long taskGroupId);
}
