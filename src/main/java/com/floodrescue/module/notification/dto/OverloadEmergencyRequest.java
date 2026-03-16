package com.floodrescue.module.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OverloadEmergencyRequest {
    private Long queueRequestId;
    private String note;
}
