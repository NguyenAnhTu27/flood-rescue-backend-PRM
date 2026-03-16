package com.floodrescue.module.rescue.controller;

import com.floodrescue.module.rescue.dto.request.AddNoteRequest;
import com.floodrescue.module.rescue.dto.request.EscalateTaskGroupRequest;
import com.floodrescue.module.rescue.dto.request.UpdateTeamLocationRequest;
import com.floodrescue.module.rescue.dto.response.RescueRequestResponse;
import com.floodrescue.module.rescue.dto.response.RescuerDashboardResponse;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.notification.dto.EmergencyAckResponse;
import com.floodrescue.module.rescue.service.RescueRequestService;
import com.floodrescue.module.rescue.service.RescuerTaskService;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rescue/rescuer")
@RequiredArgsConstructor
public class RescuerTaskController {

    private final RescueRequestService rescueRequestService;
    private final RescuerTaskService rescuerTaskService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @GetMapping("/tasks")
    public ResponseEntity<Page<RescueRequestResponse>> getMyTasks(
            @RequestParam(required = false) RescueRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RescueRequestResponse> response;
        if (status != null) {
            response = rescueRequestService.getRescueRequestsByStatus(status, pageable);
        } else {
            response = rescueRequestService.getRescueRequestsByStatus(RescueRequestStatus.IN_PROGRESS, pageable);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<RescueRequestResponse> getTaskById(@PathVariable Long id) {
        RescueRequestResponse response = rescueRequestService.getRescueRequestById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/tasks/{id}/status")
    public ResponseEntity<RescueRequestResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam RescueRequestStatus status,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        Long rescuerId = getCurrentUserId(authentication);
        RescueRequestResponse response = rescueRequestService.changeStatus(id, rescuerId, status, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{id}/notes")
    public ResponseEntity<RescueRequestResponse> addNote(
            @PathVariable Long id,
            @Valid @RequestBody AddNoteRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        RescueRequestResponse response = rescueRequestService.addNote(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<RescuerDashboardResponse> getDashboard(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.getDashboard(userId));
    }

    @GetMapping("/task-groups")
    public ResponseEntity<Page<TaskGroupResponse>> getMyTaskGroups(
            @RequestParam(required = false) TaskGroupStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.getMyTaskGroups(userId, status, pageable));
    }

    @GetMapping("/task-groups/{id}")
    public ResponseEntity<TaskGroupResponse> getMyTaskGroup(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.getMyTaskGroup(userId, id));
    }

    @PutMapping("/task-groups/{id}/status")
    public ResponseEntity<TaskGroupResponse> updateMyTaskGroupStatus(
            @PathVariable Long id,
            @RequestParam TaskGroupStatus status,
            @RequestParam(required = false) String note,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.updateMyTaskGroupStatus(userId, id, status, note));
    }

    @PostMapping("/task-groups/{id}/escalate")
    public ResponseEntity<TaskGroupResponse> escalateMyTaskGroup(
            @PathVariable Long id,
            @Valid @RequestBody EscalateTaskGroupRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.escalateMyTaskGroup(userId, id, request));
    }

    @GetMapping("/task-groups/{id}/emergency-acks")
    public ResponseEntity<List<EmergencyAckResponse>> getEmergencyAcks(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescuerTaskService.getEmergencyAcks(userId, id));
    }

    @PostMapping("/team-location")
    public ResponseEntity<?> updateTeamLocation(
            @Valid @RequestBody UpdateTeamLocationRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        rescuerTaskService.updateMyTeamLocation(
                userId,
                request.getLatitude(),
                request.getLongitude(),
                request.getLocationText()
        );
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật vị trí đội cứu hộ"));
    }

    @PostMapping("/assets/return")
    public ResponseEntity<?> returnMyTeamAssets(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        long returnedCount = rescuerTaskService.returnMyTeamAssets(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã trả tài sản về trạng thái sẵn sàng",
                "returnedAssetCount", returnedCount
        ));
    }
}
