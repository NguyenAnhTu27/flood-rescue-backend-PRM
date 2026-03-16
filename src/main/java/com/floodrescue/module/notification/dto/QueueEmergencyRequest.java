package com.floodrescue.module.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueueEmergencyRequest {
    private Boolean direct;
    private String note;
}
