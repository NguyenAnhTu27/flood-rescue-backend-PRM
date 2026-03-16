package com.floodrescue.module.notification.controller;

import com.floodrescue.module.notification.dto.NotificationResponse;
import com.floodrescue.module.notification.dto.OverloadEmergencyRequest;
import com.floodrescue.module.notification.dto.QueueEmergencyRequest;
import com.floodrescue.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(notificationService.getMyNotifications(userId, unreadOnly, pageable));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnread(userId)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markRead(userId, id);
        return ResponseEntity.ok(Map.of("message", "Đã xác nhận đã xem"));
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả là đã xem"));
    }

    @PostMapping("/{id}/queue")
    public ResponseEntity<?> queueEmergency(
            @PathVariable Long id,
            @RequestBody(required = false) QueueEmergencyRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        boolean direct = request == null || request.getDirect() == null || request.getDirect();
        String note = request != null ? request.getNote() : null;
        Long queuedRequestId = notificationService.queueEmergencyRequest(userId, id, direct, note);
        return ResponseEntity.ok(Map.of(
                "message", "Đã đưa yêu cầu khẩn cấp vào hàng đợi",
                "queuedRequestId", queuedRequestId
        ));
    }

    @PostMapping("/emergency/overload")
    public ResponseEntity<?> overloadEmergency(
            @RequestBody OverloadEmergencyRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        Long queueRequestId = request != null ? request.getQueueRequestId() : null;
        String note = request != null ? request.getNote() : null;
        notificationService.markEmergencyOverloaded(userId, queueRequestId, note);
        return ResponseEntity.ok(Map.of(
                "message", "Đã báo quá tải và giữ yêu cầu trong hàng đợi",
                "queueRequestId", queueRequestId
        ));
    }
}
