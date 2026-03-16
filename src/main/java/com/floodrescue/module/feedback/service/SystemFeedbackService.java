package com.floodrescue.module.feedback.service;

import com.floodrescue.module.feedback.dto.request.SystemFeedbackCreateRequest;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackResponse;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemFeedbackService {
    SystemFeedbackResponse createFeedback(Long citizenId, SystemFeedbackCreateRequest request);

    Page<SystemFeedbackResponse> getFeedbacks(Pageable pageable);

    SystemFeedbackSummaryResponse getSummary();
}
