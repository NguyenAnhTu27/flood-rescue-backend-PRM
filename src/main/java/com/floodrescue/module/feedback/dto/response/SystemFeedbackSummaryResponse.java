package com.floodrescue.module.feedback.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class SystemFeedbackSummaryResponse {
    private long totalFeedbacks;
    private double averageRating;
    private Map<Integer, Long> ratingDistribution;
}
