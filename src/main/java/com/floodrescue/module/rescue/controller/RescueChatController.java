package com.floodrescue.module.rescue.controller;

import com.floodrescue.module.rescue.dto.request.SendChatMessageRequest;
import com.floodrescue.module.rescue.dto.response.RescueChatMessageResponse;
import com.floodrescue.module.rescue.service.RescueChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/rescue-requests")
@RequiredArgsConstructor
public class RescueChatController {

    private final RescueChatService rescueChatService;

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        List<RescueChatMessageResponse> response = rescueChatService.getMessages(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendChatMessageRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        RescueChatMessageResponse response = rescueChatService.sendMessage(id, userId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }
}
