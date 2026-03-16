package com.floodrescue.module.rescue.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CitizenRescueConfirmationResponse {
    private Boolean rescued;
    private Long originalRequestId;
    private Long followUpRequestId;
    private String message;
}

