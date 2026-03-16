package com.floodrescue.module.rescue.mapper;

import com.floodrescue.module.rescue.dto.response.RescueRequestResponse;
import com.floodrescue.module.rescue.entity.RescueRequestAttachmentEntity;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.RescueRequestTimelineEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RescueRequestMapper {

    public RescueRequestResponse toResponse(RescueRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        RescueRequestResponse.RescueRequestResponseBuilder builder = RescueRequestResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .citizenId(entity.getCitizen().getId())
                .citizenName(entity.getCitizen().getFullName())
                .citizenPhone(entity.getCitizen().getPhone())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .affectedPeopleCount(entity.getAffectedPeopleCount())
                .description(entity.getDescription())
                .addressText(entity.getAddressText())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .locationDescription(entity.getLocationDescription())
                .locationVerified(entity.getLocationVerified())
                .waitingForTeam(entity.getWaitingForTeam())
                .coordinatorCancelNote(entity.getCoordinatorCancelNote())
                .emergency(entity.getIsEmergency())
                .emergencyNo(entity.getEmergencyNo())
                .emergencyActionStatus(null)
                .sourceTeamId(entity.getSourceTeamId())
                .emergencyParentRequestId(entity.getEmergencyParentRequestId())
                .rescueResultConfirmationStatus(entity.getRescueResultConfirmationStatus())
                .rescueResultConfirmationNote(entity.getRescueResultConfirmationNote())
                .rescueResultConfirmedAt(entity.getRescueResultConfirmedAt())
                .waitingCitizenRescueConfirmation(false)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getMasterRequest() != null) {
            builder.masterRequestId(entity.getMasterRequest().getId())
                    .masterRequestCode(entity.getMasterRequest().getCode());
        }

        return builder.build();
    }

    public RescueRequestResponse toResponseWithDetails(
            RescueRequestEntity entity,
            List<RescueRequestAttachmentEntity> attachments,
            List<RescueRequestTimelineEntity> timeline) {

        RescueRequestResponse response = toResponse(entity);

        if (attachments != null && !attachments.isEmpty()) {
            response.setAttachments(attachments.stream()
                    .map(this::toAttachmentResponse)
                    .collect(Collectors.toList()));
        }

        if (timeline != null && !timeline.isEmpty()) {
            response.setTimeline(timeline.stream()
                    .map(this::toTimelineResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private RescueRequestResponse.AttachmentResponse toAttachmentResponse(RescueRequestAttachmentEntity entity) {
        return RescueRequestResponse.AttachmentResponse.builder()
                .id(entity.getId())
                .fileUrl(entity.getFileUrl())
                .fileType(entity.getFileType())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private RescueRequestResponse.TimelineResponse toTimelineResponse(RescueRequestTimelineEntity entity) {
        return RescueRequestResponse.TimelineResponse.builder()
                .id(entity.getId())
                .actorId(entity.getActor().getId())
                .actorName(entity.getActor().getFullName())
                .eventType(entity.getEventType().name())
                .fromStatus(entity.getFromStatus() != null ? entity.getFromStatus().name() : null)
                .toStatus(entity.getToStatus() != null ? entity.getToStatus().name() : null)
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
