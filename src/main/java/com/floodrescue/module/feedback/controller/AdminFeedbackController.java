package com.floodrescue.module.feedback.controller;

import com.floodrescue.module.feedback.dto.response.SystemFeedbackResponse;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackSummaryResponse;
import com.floodrescue.module.feedback.service.SystemFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final SystemFeedbackService systemFeedbackService;

    @GetMapping
    public ResponseEntity<Page<SystemFeedbackResponse>> getFeedbacks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(systemFeedbackService.getFeedbacks(pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<SystemFeedbackSummaryResponse> getSummary() {
        return ResponseEntity.ok(systemFeedbackService.getSummary());
    }
}
