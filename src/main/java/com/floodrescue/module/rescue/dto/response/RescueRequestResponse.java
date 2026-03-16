package com.floodrescue.module.rescue.dto.response;

import com.floodrescue.shared.enums.AttachmentFileType;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RescueRequestResponse {

    private Long id;
    private String code;
    private Long citizenId;
    private String citizenName;
    private String citizenPhone;

    private RescueRequestStatus status;
    private RescuePriority priority;

    private Long masterRequestId;
    private String masterRequestCode;

    private Integer affectedPeopleCount;
    private String description;
    private String addressText;
    private Double latitude;
    private Double longitude;
    private String locationDescription;
    private Boolean locationVerified;
    private Boolean waitingForTeam;
    private String coordinatorCancelNote;
    private Boolean emergency;
    private Integer emergencyNo;
    private String emergencyActionStatus;
    private Long sourceTeamId;
    private Long emergencyParentRequestId;
    private String rescueResultConfirmationStatus;
    private String rescueResultConfirmationNote;
    private LocalDateTime rescueResultConfirmedAt;
    private Boolean waitingCitizenRescueConfirmation;

    private List<AttachmentResponse> attachments;
    private List<TimelineResponse> timeline;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    public static class AttachmentResponse {
        private Long id;
        private String fileUrl;
        private AttachmentFileType fileType;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class TimelineResponse {
        private Long id;
        private Long actorId;
        private String actorName;
        private String eventType;
        private String fromStatus;
        private String toStatus;
        private String note;
        private LocalDateTime createdAt;
    }
}
