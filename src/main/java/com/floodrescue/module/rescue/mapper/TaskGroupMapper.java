package com.floodrescue.module.rescue.mapper;

import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.rescue.entity.RescueAssigmentEntity;
import com.floodrescue.module.rescue.entity.TaskGroupEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.entity.TaskGroupTimelineEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskGroupMapper {

    public TaskGroupResponse toResponse(TaskGroupEntity entity) {
        if (entity == null) return null;

        TaskGroupResponse.TaskGroupResponseBuilder builder = TaskGroupResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .status(entity.getStatus())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getAssignedTeam() != null) {
            builder.assignedTeamId(entity.getAssignedTeam().getId())
                    .assignedTeamName(entity.getAssignedTeam().getName());
        }

        if (entity.getCreatedBy() != null) {
            builder.createdById(entity.getCreatedBy().getId())
                    .createdByName(entity.getCreatedBy().getFullName());
        }

        return builder.build();
    }

    public TaskGroupResponse toResponseWithDetails(
            TaskGroupEntity entity,
            List<TaskGroupRequestEntity> requests,
            List<RescueAssigmentEntity> assignments,
            List<TaskGroupTimelineEntity> timeline
    ) {
        TaskGroupResponse response = toResponse(entity);

        if (requests != null && !requests.isEmpty()) {
            response.setRequests(
                    requests.stream()
                            .map(this::toRequestItem)
                            .collect(Collectors.toList())
            );
        }

        if (assignments != null && !assignments.isEmpty()) {
            response.setAssignments(
                    assignments.stream()
                            .map(this::toAssignmentItem)
                            .collect(Collectors.toList())
            );
        }

        if (timeline != null && !timeline.isEmpty()) {
            response.setTimeline(
                    timeline.stream()
                            .map(this::toTimelineItem)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    private TaskGroupResponse.TaskGroupRequestItem toRequestItem(TaskGroupRequestEntity entity) {
        var request = entity.getRescueRequest();
        var citizen = request.getCitizen();
        return TaskGroupResponse.TaskGroupRequestItem.builder()
                .id(request.getId())
                .code(request.getCode())
                .status(request.getStatus().name())
                .priority(request.getPriority().name())
                .affectedPeopleCount(request.getAffectedPeopleCount())
                .addressText(request.getAddressText())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationDescription(request.getLocationDescription())
                .description(request.getDescription())
                .locationVerified(request.getLocationVerified())
                .citizenId(citizen != null ? citizen.getId() : null)
                .citizenName(citizen != null ? citizen.getFullName() : null)
                .citizenPhone(citizen != null ? citizen.getPhone() : null)
                .emergency(request.getIsEmergency())
                .emergencyNo(request.getEmergencyNo())
                .sourceTeamId(request.getSourceTeamId())
                .emergencyParentRequestId(request.getEmergencyParentRequestId())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private TaskGroupResponse.AssignmentItem toAssignmentItem(RescueAssigmentEntity entity) {
        TaskGroupResponse.AssignmentItem.AssignmentItemBuilder builder =
                TaskGroupResponse.AssignmentItem.builder()
                        .id(entity.getId())
                        .teamId(entity.getTeam().getId())
                        .teamName(entity.getTeam().getName())
                        .assignedAt(entity.getAssignedAt())
                        .active(entity.getIsActive());

        if (entity.getAsset() != null) {
            builder.assetId(entity.getAsset().getId())
                    .assetCode(entity.getAsset().getCode())
                    .assetName(entity.getAsset().getName());
        }

        if (entity.getAssignedBy() != null) {
            builder.assignedById(entity.getAssignedBy().getId())
                    .assignedByName(entity.getAssignedBy().getFullName());
        }

        return builder.build();
    }

    private TaskGroupResponse.TimelineItem toTimelineItem(TaskGroupTimelineEntity entity) {
        var actor = entity.getActor();
        return TaskGroupResponse.TimelineItem.builder()
                .id(entity.getId())
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actor != null ? actor.getFullName() : "Hệ thống")
                .eventType(entity.getEventType())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
