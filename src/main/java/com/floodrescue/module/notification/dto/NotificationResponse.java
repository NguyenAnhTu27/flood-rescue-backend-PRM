package com.floodrescue.module.notification.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String content;
    private Boolean read;
    private Boolean urgent;
    private String eventCode;
    private String referenceType;
    private Long referenceId;
    private String actionStatus;
    private String actionNote;
    private Long queueRequestId;
    private Long sourceTeamId;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
