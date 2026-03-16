package com.floodrescue.module.rescue.dto.response;

import com.floodrescue.shared.enums.TaskGroupStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TaskGroupResponse {

    private Long id;
    private String code;
    private TaskGroupStatus status;

    private Long assignedTeamId;
    private String assignedTeamName;

    private String note;

    private Long createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TaskGroupRequestItem> requests;
    private List<AssignmentItem> assignments;
    private List<TimelineItem> timeline;

    @Getter
    @Setter
    @Builder
    public static class TaskGroupRequestItem {
        private Long id;
        private String code;
        private String status;
        private String priority;
        private Integer affectedPeopleCount;
        private String addressText;
        private Double latitude;
        private Double longitude;
        private String locationDescription;
        private String description;
        private Boolean locationVerified;
        private Long citizenId;
        private String citizenName;
        private String citizenPhone;
        private Boolean emergency;
        private Integer emergencyNo;
        private Long sourceTeamId;
        private Long emergencyParentRequestId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    public static class AssignmentItem {
        private Long id;
        private Long teamId;
        private String teamName;
        private Long assetId;
        private String assetCode;
        private String assetName;
        private Long assignedById;
        private String assignedByName;
        private LocalDateTime assignedAt;
        private Boolean active;
    }

    @Getter
    @Setter
    @Builder
    public static class TimelineItem {
        private Long id;
        private Long actorId;
        private String actorName;
        private String eventType;
        private String note;
        private LocalDateTime createdAt;
    }
}
