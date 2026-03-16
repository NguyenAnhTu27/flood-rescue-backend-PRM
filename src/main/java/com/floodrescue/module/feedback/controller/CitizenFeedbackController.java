package com.floodrescue.module.feedback.controller;

import com.floodrescue.module.feedback.dto.request.SystemFeedbackCreateRequest;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackResponse;
import com.floodrescue.module.feedback.service.SystemFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback/citizen")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CITIZEN')")
public class CitizenFeedbackController {

    private final SystemFeedbackService systemFeedbackService;

    @PostMapping
    public ResponseEntity<SystemFeedbackResponse> createFeedback(
            @Valid @RequestBody SystemFeedbackCreateRequest request,
            Authentication authentication
    ) {
        Long citizenId = getCurrentUserId(authentication);
        return ResponseEntity.ok(systemFeedbackService.createFeedback(citizenId, request));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }
}
