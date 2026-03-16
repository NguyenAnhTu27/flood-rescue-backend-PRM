package com.floodrescue.module.notification.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EmergencyAckResponse {
    private Long coordinatorId;
    private String coordinatorName;
    private Boolean read;
    private String actionStatus;
    private String actionNote;
    private Long queueRequestId;
    private LocalDateTime acknowledgedAt;
}
