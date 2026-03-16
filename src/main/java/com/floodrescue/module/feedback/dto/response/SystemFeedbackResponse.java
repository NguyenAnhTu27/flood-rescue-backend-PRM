package com.floodrescue.module.feedback.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemFeedbackResponse {
    private Long id;
    private Long citizenId;
    private String citizenName;
    private String citizenEmail;
    private Integer rating;
    private String feedbackContent;
    private Boolean rescuedConfirmed;
    private Boolean reliefConfirmed;
    private LocalDateTime createdAt;
}
